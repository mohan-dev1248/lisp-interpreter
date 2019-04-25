class Environment(val outerScope: Environment? = null) {

    var envMap = mutableMapOf<String, Any>()

    fun init(params: List<String>, args: List<Double>) {
        if (params.size == args.size)
            for (i in 0 until params.size)
                envMap[params[i]] = args[i]
    }

    fun init(envMap: MutableMap<String, Any>) {
        this.envMap = envMap
    }

    fun find(key: String): Any? {
        return if (envMap[key] != null) envMap[key] else outerScope?.find(key)
    }

    fun add(key: String, value: Any) {
        envMap[key] = value
    }

    fun replace(key: String, value: Any) {
        envMap.replace(key, value)
    }
}