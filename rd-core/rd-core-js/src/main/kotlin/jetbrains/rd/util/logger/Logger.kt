package jetbrains.rd.util.logger

impl object LoggerFactory {

    impl fun logger(name: String): Logger = object : Logger() {

        override fun log(message: String, level: LogLevel) {
            
        }

    }
}
