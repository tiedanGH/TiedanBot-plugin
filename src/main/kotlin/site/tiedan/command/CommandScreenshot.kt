package site.tiedan.command

import com.microsoft.playwright.*
import com.microsoft.playwright.options.LoadState
import kotlinx.coroutines.sync.Mutex
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.message.data.*
import site.tiedan.TiedanGame
import site.tiedan.TiedanGame.Command
import site.tiedan.TiedanGame.adminOnly
import site.tiedan.TiedanGame.baseDataFolder
import site.tiedan.TiedanGame.logger
import site.tiedan.TiedanGame.save
import site.tiedan.TiedanGame.sendQuoteReply
import site.tiedan.TiedanGame.uploadFileToImage
import site.tiedan.config.BotConfig
import site.tiedan.data.AdminListData
import site.tiedan.data.DomainWhiteListData
import site.tiedan.module.UrlWhitelistValidator
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Paths
import kotlin.collections.contains
import kotlin.io.path.createDirectories

object CommandScreenshot : RawCommand(
    owner = TiedanGame,
    primaryName = "screenshot",
    secondaryNames = arrayOf("截图", "ss"),
    description = "网页截图相关指令",
    usage = "${commandPrefix}screenshot help"
){
    private val commandList = listOf(
        Command("ss <URL>", "截图 <URL>", "指定链接截图", 1),
        Command("ss list", "截图 列表", "查看白名单列表", 1),

        Command("ss add <domain>", "截图 添加 <域名>", "添加白名单", 2),
        Command("ss remove <domain>", "截图 移除 <域名>", "移除白名单", 2),
    )

    private val lock = Mutex()


    override suspend fun CommandSender.onCommand(args: MessageChain) {

        val isAdmin = AdminListData.AdminList.contains(user?.id) || user?.id == BotConfig.master || isConsole()

        try {
            when (args[0].content) {

                "help"-> {   // 查看screenshot可用帮助（help）
                    var reply = " ·📸 截图指令帮助：\n" +
                            commandList.filter { it.type == 1 }.joinToString("") { "${commandPrefix}${it.usage}　${it.desc}\n" }
                    if (isAdmin) {
                        reply += " ·🛠️ admin管理指令：\n" +
                            commandList.filter { it.type == 2 }.joinToString("") { "${commandPrefix}${it.usage}　${it.desc}\n" }
                    }
                    sendQuoteReply(reply)
                }

                "帮助"-> {   // 查看screenshot可用帮助（帮助）
                    var reply = " ·📸 截图指令帮助：\n" +
                            commandList.filter { it.type == 1 }.joinToString("") { "${commandPrefix}${it.usageCN}　${it.desc}\n" }
                    if (isAdmin) {
                        reply += " ·🛠️ admin管理指令：\n" +
                            commandList.filter { it.type == 2 }.joinToString("") { "${commandPrefix}${it.usageCN}　${it.desc}\n" }
                    }
                    sendQuoteReply(reply)
                }

                "list", "列表"-> {   // 查看白名单列表
                    var domainList = "·白名单列表："
                    for (domain in DomainWhiteListData.WhiteList) {
                        domainList += "\n$domain"
                    }
                    sendQuoteReply(domainList)
                }

                "add", "添加"-> {   // 添加白名单
                    adminOnly(this)
                    val domain = args[1].content
                    if (UrlWhitelistValidator.isUnsafeHost(domain)) {
                        sendQuoteReply("安全限制：禁止添加内网地址到白名单列表")
                        return
                    }
                    val result = DomainWhiteListData.WhiteList.add(domain)
                    if (result) {
                        DomainWhiteListData.WhiteList = DomainWhiteListData.WhiteList.toSortedSet()
                        DomainWhiteListData.save()
                        sendQuoteReply("已添加白名单 $domain")
                    } else {
                        sendQuoteReply("白名单 $domain 已存在")
                    }
                }

                "remove", "rm", "移除"-> {   // 移除白名单
                    adminOnly(this)
                    val domain = args[1].content
                    val result = DomainWhiteListData.WhiteList.remove(domain)
                    if (result) {
                        DomainWhiteListData.save()
                        sendQuoteReply("已移除白名单 $domain")
                    } else {
                        sendQuoteReply("白名单 $domain 不存在")
                    }
                }

                else-> {    // 默认截图操作
                    val url = args[0].content
                    if (!UrlWhitelistValidator.isAllowed(url)) {
                        sendQuoteReply("访问受限：链接格式错误或不在白名单内，请联系管理员添加白名单")
                        return
                    }
                    @OptIn(ConsoleExperimentalApi::class)
                    val outputPath = "$baseDataFolder/cache/screenshot.png"

                    try {
                        // 调用 Playwright 进行截图
                        logger.info("执行 Playwright 网页截图")
                        PlaywrightScreenshot.screenshot(url, outputPath)
                    } catch (_: TimeoutError) {
                        sendQuoteReply("[错误] 截图失败：Playwright执行超时，最大加载时间限制为60秒")
                        return
                    } catch (e: Exception) {
                        logger.warning(e)
                        sendQuoteReply("[错误] 截图失败：Playwright执行出错，请联系管理员查看后台日志")
                        return
                    }

                    val image = subject?.uploadFileToImage(File(outputPath))
                        ?: return sendQuoteReply("[错误] 图片文件异常：ExternalResource上传失败，请尝试重新执行")
                    sendMessage(image)
                }
            }
        } catch (e: PermissionDeniedException) {
            sendQuoteReply("[操作无效] ${e.message}")
        } catch (_: IndexOutOfBoundsException) {
            sendQuoteReply("[操作无效] 未知的参数")
        } catch (e: Exception) {
            logger.warning(e)
            sendQuoteReply("[指令执行未知错误]\n可能由于bot发消息出错，请联系铁蛋查看后台：${e::class.simpleName}(${e.message})")
        }
    }

    private object PlaywrightScreenshot {
        suspend fun screenshot(
            url: String,
            outputPath: String,
            fullPage: Boolean = true,
            timeout: Double = 60_000.0
        ) {
            Paths.get(outputPath).parent?.createDirectories()

            val driverJarPath = "plugin-libraries/com/microsoft/playwright/driver/1.42.0/driver-1.42.0.jar"
            val driverJarFile = File(driverJarPath)
            if (!driverJarFile.exists()) {
                throw RuntimeException("driver.jar 文件不存在: $driverJarPath")
            }

            val driverUrl = driverJarFile.toURI().toURL()
            val loader = URLClassLoader(arrayOf(driverUrl), PlaywrightScreenshot::class.java.classLoader)
            // 设置线程上下文 classloader
            val originalLoader = Thread.currentThread().contextClassLoader
            Thread.currentThread().contextClassLoader = loader

            try {
                lock.lock()
                // 启动 Playwright
                Playwright.create().use { playwright ->
                    val browser = playwright.chromium().launch(
                        BrowserType.LaunchOptions()
                            .setHeadless(true)
                            .setArgs(listOf("--no-sandbox", "--disable-setuid-sandbox"))
                    )

                    val context = browser.newContext(
                        Browser.NewContextOptions()
                            .setViewportSize(1280, 800)
                    )

                    val page = context.newPage()
                    page.setDefaultTimeout(timeout)

                    // 打开页面
                    page.navigate(url)
                    // 等待页面稳定
                    page.waitForLoadState(LoadState.LOAD)
                    // 截图
                    page.screenshot(
                        Page.ScreenshotOptions()
                            .setPath(Paths.get(outputPath))
                            .setFullPage(fullPage)
                    )

                    context.close()
                    browser.close()
                }
            } finally {
                lock.unlock()
                // 恢复原 classloader
                Thread.currentThread().contextClassLoader = originalLoader
            }
        }
    }
}
