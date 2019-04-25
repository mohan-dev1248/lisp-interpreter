class Procedure(vararg val paramList: String, val body: String) {

    fun call(vararg args: Double, outerEnv: Environment): Pair<Any, String>? {
        if (paramList.size == args.size) {
            val env: Environment = Environment(outerEnv)
            env.init(paramList.toList(), args.toList())
            return evalExpression(env,body)
        } else {
            throw error("Illegal arguments for the Function")
        }
    }
}