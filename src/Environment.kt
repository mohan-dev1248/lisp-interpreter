class Environment(val outerScope: Environment? = null) {

    var envMap = mutableMapOf<String, Any>()

    fun init(params: List<String>, args: List<Double>) {
        if (params.size == args.size)
            for (i in 0..params.size - 1)
                envMap.put(params.get(i), args.get(i))
    }

    fun init(envMap: MutableMap<String, Any>) {
        this.envMap = envMap
    }

    fun find(key: String): Any? {
        return if (envMap.get(key) != null) envMap.get(key) else outerScope?.find(key)
    }

    fun add(key: String, value: Any) {
        envMap.put(key, value)
    }

    fun replace(key: String, value: Any) {
        envMap.replace(key, value)
    }
}