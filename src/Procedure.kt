class Procedure(vararg val paramList: String, val body: String) {

    fun call(vararg args: Any, outerEnv: Environment): Pair<Any, String>? {
        return if (paramList.size == args.size) {
            val env = Environment(outerEnv)
            env.init(paramList.toList(), args.toList())
            evalExpression(env,body)
        } else null
    }
}