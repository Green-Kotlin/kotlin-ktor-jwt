package call.green

import call.green.auth.JwtService
import call.green.auth.MySession
import call.green.auth.hash
import call.green.plugins.configureRouting
import call.green.repository.DatabaseFactory
import call.green.repository.TodoRepository
import call.green.routes.users
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.locations.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.sessions.*

const val API_VERSION = "/v1"

data class UserSession(val id: String, val count: Int)

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(Locations)
        install(Sessions) {
            cookie<MySession>("user_session")
        }

        DatabaseFactory.init()
        val db = TodoRepository()
        val jwtService = JwtService()
        val hashFunction = { s: String -> hash(s) }

        install(Authentication) {
            jwt("jwt") { //1
                verifier(jwtService.verifier) // 2
                realm = "Todo Server"
                validate { // 3
                    val payload = it.payload
                    val claim = payload.getClaim("id")
                    val claimString = claim.asInt()
                    val user = db.findUser(claimString) // 4
                    user
                }
            }
        }
        routing {
            users(db, jwtService, hashFunction)
        }
        configureRouting()
    }.start(wait = true)


}

