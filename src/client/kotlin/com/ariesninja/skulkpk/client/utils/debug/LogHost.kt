package com.ariesninja.skulkpk.client.utils.debug

import com.ariesninja.skulkpk.client.event.EventListener
import org.apache.logging.log4j.LogManager

/**
 * LiquidBounce
 *
 * A free mixin-based injection hacked-client for Minecraft using FabricMC.
 *
 * @author kawaiinekololis (@team CCBlueX)
 */
object LogHost : EventListener {

    /**
     * CLIENT INFORMATION
     *
     * WARNING: Please read the GNU General Public License
     */
//    const val CLIENT_NAME = "LiquidBounce"
//    const val CLIENT_AUTHOR = "CCBlueX"
//
//    private object Client : Configurable("Client") {
//        val version = text("Version", gitInfo["git.build.version"]?.toString() ?: "unknown").immutable()
//        val commit = text("Commit", gitInfo["git.commit.id.abbrev"]?.let { "git-$it" } ?: "unknown").immutable()
//        val branch = text("Branch", gitInfo["git.branch"]?.toString() ?: "nextgen").immutable()
//
//        init {
//            ConfigSystem.root(this)
//
//            version.onChange { previousVersion ->
//                runCatching {
//                    ConfigSystem.backup("automatic_${previousVersion}-${version.inner}")
//                }.onFailure {
//                    logger.error("Unable to create backup", it)
//                }
//
//                previousVersion
//            }
//        }
//    }
//
//    val clientVersion by Client.version
//    val clientCommit by Client.commit
//    val clientBranch by Client.branch
//
//    /**
//     * Defines if the client is in development mode.
//     * This will enable update checking on commit time instead of semantic versioning.
//     *
//     * TODO: Replace this approach with full semantic versioning.
//     */
//    const val IN_DEVELOPMENT = true
//
//    /**
//     * Client logger to print out console messages
//     */
    val logger = LogManager.getLogger("Skulk")!!

//    var taskManager: TaskManager? = null
//
//    var isInitialized = false
//        private set
//
//    /**
//     * Initializes the client, called when
//     * we reached the last stage of the splash screen.
//     *
//     * The thread should be the main render thread.
//     */
//    private fun initializeClient() {
//        if (isInitialized) {
//            return
//        }
//
//        // Ensure we are on the render thread
//        RenderSystem.assertOnRenderThread()
//
//        // Initialize managers and features
//        Client
//        initializeManagers()
//        initializeFeatures()
//        initializeResources()
//        prepareGuiStage()
//
//        // Register shutdown hook in case [ClientShutdownEvent] is not called
//        Runtime.getRuntime().addShutdownHook(Thread(::shutdownClient))
//
//        // Check for AMD Vega iGPU
//        if (HAS_AMD_VEGA_APU) {
//            logger.info("AMD Vega iGPU detected, enabling different line smooth handling. " +
//                "If you believe this is a mistake, please create an issue at " +
//                "https://github.com/CCBlueX/LiquidBounce/issues.")
//        }
//
//        // Do backup before loading configs
//        if (!ConfigSystem.isFirstLaunch && !Client.jsonFile.exists()) {
//            runCatching {
//                ConfigSystem.backup("automatic_${Client.version.inner}")
//            }.onFailure {
//                logger.error("Unable to create backup", it)
//            }
//        }
//
//        // Load all configurations
//        ConfigSystem.loadAll()
//
//        isInitialized = true
//    }
//
//    /**
//     * Initializes managers for Event Listener registration.
//     */
//    private fun initializeManagers() {
//        // Config
//        ConfigSystem
//
//        // Utility
//        RenderedEntities
//        ChunkScanner
//        InputTracker
//
//        // Feature managers
//        ModuleManager
//        CommandManager
//        ProxyManager
//        AccountManager
//
//        // Script system
//        EnvironmentRemapper
//        runCatching(ScriptManager::initializeEngine).onFailure { error ->
//            logger.error("[ScriptAPI] Failed to initialize script engine.", error)
//        }
//
//        // Utility managers
//        RotationManager
//        PacketQueueManager
//        BacktrackPacketManager
//        InteractionTracker
//        CombatManager
//        FriendManager
//        InventoryManager
//        WorldToScreen
//        ActiveServerList
//        ConfigSystem.root(ClientItemGroups)
//        ConfigSystem.root(LanguageManager)
//        ConfigSystem.root(ClientAccountManager)
//        ConfigSystem.root(SpooferManager)
//        PostRotationExecutor
//        ServerObserver
//        ItemImageAtlas
//    }
//
//    /**
//     * Initializes in-built and script features.
//     */
//    private fun initializeFeatures() {
//        // Register commands and modules
//        CommandManager.registerInbuilt()
//        ModuleManager.registerInbuilt()
//
//        // Load user scripts
//        runCatching(ScriptManager::loadAll).onFailure { error ->
//            logger.error("ScriptManager was unable to load scripts.", error)
//        }
//    }
//
//    /**
//     * Simultaneously initializes resources
//     * such as translations, cosmetics, player heads, configs and so on,
//     * which do not rely on the main thread.
//     */
//    private fun initializeResources() = runBlocking {
//        logger.info("Initializing API...")
//        // Lookup API config
//        ApiConfig.config
//
//        listOf(
//            scope.async {
//                // Load translations
//                LanguageManager.loadDefault()
//            },
//            scope.async {
//                val update = update ?: return@async
//                logger.info("[Update] Update available: $clientVersion -> ${update.lbVersion}")
//            },
//            scope.async {
//                // Load cosmetics
//                CosmeticService.refreshCarriers(force = true) {
//                    logger.info("Successfully loaded ${CosmeticService.carriers.size} cosmetics carriers.")
//                }
//            },
//            scope.async {
//                // Download player heads
//                heads
//            },
//            scope.async {
//                // Load configs
//                AutoConfig.reloadConfigs()
//            },
//            scope.async {
//                // IPC configuration
//                ipcConfiguration
//            },
//            scope.async {
//                IpInfoApi.original
//            },
//            scope.async {
//                if (ClientAccountManager.clientAccount != ClientAccount.EMPTY_ACCOUNT) {
//                    runCatching {
//                        ClientAccountManager.clientAccount.renew()
//                    }.onFailure {
//                        logger.error("Failed to renew client account token.", it)
//                        ClientAccountManager.clientAccount = ClientAccount.EMPTY_ACCOUNT
//                    }.onSuccess {
//                        logger.info("Successfully renewed client account token.")
//                        ConfigSystem.storeConfigurable(ClientAccountManager)
//                    }
//                }
//            },
//            scope.async {
//                ThemeManager.themesFolder.listFiles()
//                    ?.filter { file -> file.isDirectory }
//                    ?.forEach { file ->
//                        runCatching {
//                            val assetsFolder = File(file, "assets")
//                            if (!assetsFolder.exists()) {
//                                return@forEach
//                            }
//
//                            FontManager.queueFolder(assetsFolder)
//                        }.onFailure {
//                            logger.error("Failed to queue fonts from theme '${file.name}'.", it)
//                        }
//                    }
//            }
//        ).awaitAll()
//    }
//
//    /**
//     * Prepares the GUI stage of the client.
//     * This will load [ThemeManager], as well as the [BrowserBackendManager] and [ClientInteropServer].
//     */
//    private fun prepareGuiStage() {
//        // Load theme and component overlay
//        ThemeManager
//        BrowserBackendManager
//
//        // Start Interop Server
//        ClientInteropServer.start()
//        IntegrationListener
//
//        taskManager = TaskManager(scope).apply {
//            // Either immediately starts browser or spawns a task to request browser dependencies,
//            // and then starts the browser through render thread.
//            BrowserBackendManager.makeDependenciesAvailable(this)
//
//            // Initialize deep learning engine as task, because we cannot know if DJL will request
//            // resources from the internet.
//            launch("Deep Learning") { task ->
//                runCatching {
//                    DeepLearningEngine.init(task)
//                    ModelHolster.load()
//                }.onFailure { exception ->
//                    task.subTasks.clear()
//
//                    // LiquidBounce can still run without deep learning,
//                    // and we don't want to crash the client if it fails.
//                    logger.info("Failed to initialize deep learning.", exception)
//                }
//            }
//        }
//
//        // Prepare glyph manager
//        val duration = measureTime {
//            FontManager.createGlyphManager()
//        }
//        logger.info("Completed loading fonts in ${duration.inWholeMilliseconds} ms.")
//        logger.info("Fonts: [ ${FontManager.fontFaces.joinToString { face -> face.name }} ]")
//
//        // Insert default components on HUD
//        ComponentOverlay.insertDefaultComponents()
//    }
//
//    /**
//     * Shuts down the client. This will save all configurations and stop all running tasks.
//     */
//    private fun shutdownClient() {
//        if (!isInitialized) {
//            return
//        }
//        isInitialized = false
//        logger.info("Shutting down client...")
//
//        // Unregister all event listener and stop all running tasks
//        ChunkScanner.ChunkScannerThread.stopThread()
//        EventManager.unregisterAll()
//
//        // Save all configurations
//        ConfigSystem.storeAll()
//
//        // Shutdown browser as last step
//        BrowserBackendManager.stop()
//    }
//
//    /**
//     * Should be executed to start the client.
//     */
//    @Suppress("unused")
//    private val startHandler = handler<ClientStartEvent> {
//        runCatching {
//            logger.info("Launching $CLIENT_NAME v$clientVersion by $CLIENT_AUTHOR")
//            // Print client information
//            logger.info("Client Version: $clientVersion ($clientCommit)")
//            logger.info("Client Branch: $clientBranch")
//            logger.info("Operating System: ${System.getProperty("os.name")} (${System.getProperty("os.version")})")
//            logger.info("Java Version: ${System.getProperty("java.version")}")
//            logger.info("Screen Resolution: ${mc.window.width}x${mc.window.height}")
//            logger.info("Refresh Rate: ${mc.window.refreshRate} Hz")
//
//            // Initialize event manager
//            EventManager
//
//            // Register resource reloader
//            val resourceManager = mc.resourceManager
//            val clientInitializer = ClientInitializer()
//            if (resourceManager is ReloadableResourceManagerImpl) {
//                resourceManager.registerReloader(clientInitializer)
//            } else {
//                logger.warn("Failed to register resource reloader!")
//
//                // Run resource reloader directly as fallback
//                clientInitializer.reload(resourceManager)
//            }
//        }.onFailure {
//            ErrorHandler.fatal(it, additionalMessage = "Client start")
//        }
//    }
//
//    @Suppress("unused")
//    private val screenHandler = handler<ScreenEvent>(priority = FIRST_PRIORITY) { event ->
//        val taskManager = taskManager ?: return@handler
//
//        if (!taskManager.isCompleted && event.screen !is TaskProgressScreen) {
//            event.cancelEvent()
//            mc.setScreen(TaskProgressScreen("Loading Required Libraries", taskManager))
//        }
//    }
//
//    /**
//     * Resource reloader which is executed on client start and reload.
//     * This is used to run async tasks without blocking the main thread.
//     *
//     * For now this is only used to check for updates and request additional information from the internet.
//     *
//     * @see SynchronousResourceReloader
//     * @see ResourceReloader
//     */
//    class ClientInitializer : SynchronousResourceReloader {
//        override fun reload(manager: ResourceManager) {
//            runCatching(::initializeClient).onSuccess {
//                logger.info("$CLIENT_NAME has been successfully initialized.")
//            }.onFailure {
//                ErrorHandler.fatal(it, additionalMessage = "Client resource reloader")
//            }
//        }
//    }
//
//    /**
//     * Should be executed to stop the client.
//     */
//    @Suppress("unused")
//    private val shutdownHandler = handler<ClientShutdownEvent> {
//        shutdownClient()
//    }


}