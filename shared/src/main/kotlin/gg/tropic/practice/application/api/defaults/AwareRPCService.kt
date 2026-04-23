package gg.tropic.practice.application.api.defaults

import gg.scala.aware.Aware
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import net.evilblock.cubed.serializers.Serializers
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

interface AwareConversationMessage {
    val uniqueId: UUID
}

interface AwareConversationMessageResponse {
    val uniqueId: UUID
}

interface AwareRPCContext<Response : AwareConversationMessageResponse> {
    fun reply(response: Response)
}

interface AwareRPCHandler<Request : AwareConversationMessage, Response : AwareConversationMessageResponse> {

    fun handle(request: Request, context: AwareRPCContext<Response>)
}

interface AwareRPCService<Request : AwareConversationMessage, Response : AwareConversationMessageResponse> {

    fun addHandler(handler: AwareRPCHandler<Request, Response>)

    fun call(request: Request): CompletableFuture<Response>

    fun callSync(request: Request): Response?
}

@PublishedApi
internal class AwareRPCServiceImpl<Request : AwareConversationMessage, Response : AwareConversationMessageResponse>
internal constructor(
    private val aware: Aware<AwareMessage>,
    private val serviceName: String,
    private val timeoutSeconds: Long,
    private val requestClass: Class<Request>,
    private val responseClass: Class<Response>
) : AwareRPCService<Request, Response> {
    private val handlers = mutableListOf<AwareRPCHandler<Request, Response>>()
    private val pendingRequests = ConcurrentHashMap<UUID, CompletableFuture<Response>>()

    init {

        aware.listen(serviceName) {
            try {
                val uniqueId = retrieve<UUID>("uniqueId")
                val requestJson = retrieve<String>("request")

                
                val request = Serializers.gson.fromJson(requestJson, requestClass)
                val handler = handlers.firstOrNull()
                if (handler != null) {
                    val context = object : AwareRPCContext<Response> {
                        override fun reply(response: Response) {

                            sendResponse(uniqueId, response)
                        }
                    }
                    handler.handle(request, context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        
        aware.listen("$serviceName-response") {
            try {
                val uniqueId = retrieve<UUID>("uniqueId")
                val responseJson = retrieve<String>("response")

                val future = pendingRequests.remove(uniqueId)
                if (future != null) {
                    val response = Serializers.gson.fromJson(responseJson, responseClass)
                    future.complete(response)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                pendingRequests.remove(retrieve<UUID>("uniqueId"))?.completeExceptionally(e)
            }
        }
    }

    private fun sendResponse(requestId: UUID, response: Response) {
        AwareMessage.of(
            "$serviceName-response",
            aware,
            "uniqueId" to requestId,
            "response" to Serializers.gson.toJson(response)
        ).publish(AwareThreadContext.SYNC)
    }

    override fun addHandler(handler: AwareRPCHandler<Request, Response>) {
        handlers.add(handler)
    }

    override fun call(request: Request): CompletableFuture<Response> {
        val future = CompletableFuture<Response>()
        pendingRequests[request.uniqueId] = future

        
        java.util.Timer().schedule(object : java.util.TimerTask() {
            override fun run() {
                if (pendingRequests.remove(request.uniqueId) != null) {
                    future.completeExceptionally(
                        java.util.concurrent.TimeoutException("RPC call timed out after ${timeoutSeconds} seconds")
                    )
                }
            }
        }, timeoutSeconds * 1000)

        
        AwareMessage.of(
            serviceName,
            aware,
            "uniqueId" to request.uniqueId,
            "request" to Serializers.gson.toJson(request)
        ).publish(AwareThreadContext.SYNC)

        return future
    }

    override fun callSync(request: Request): Response? {
        return try {
            call(request).get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (e: Exception) {
            null
        }
    }
}

@PublishedApi
internal fun <Request : AwareConversationMessage, Response : AwareConversationMessageResponse> DPSRedisService.createAwareRPCService(
    serviceName: String,
    timeoutSeconds: Long = 5L,
    requestClass: Class<Request>,
    responseClass: Class<Response>
): AwareRPCService<Request, Response> {
    this.start()
    val awareInstance = this.configure { this }
    return AwareRPCServiceImpl(
        awareInstance, 
        serviceName, 
        timeoutSeconds,
        requestClass,
        responseClass
    )
}

inline fun <reified Request : AwareConversationMessage, reified Response : AwareConversationMessageResponse> DPSRedisService.createAwareRPCService(
    serviceName: String,
    timeoutSeconds: Long = 5L
): AwareRPCService<Request, Response> {
    return createAwareRPCService(serviceName, timeoutSeconds, Request::class.java, Response::class.java)
}

inline fun <reified Request : AwareConversationMessage, reified Response : AwareConversationMessageResponse> DPSRedisService.createAwareRPCService(
    serviceName: String
): AwareRPCService<Request, Response> {
    return createAwareRPCService(serviceName, 5L)
}

fun <Request : AwareConversationMessage, Response : AwareConversationMessageResponse> AwareRPCService<Request, Response>.addFunctionalHandler(
    handler: (Request, AwareRPCContext<Response>) -> Unit
) {
    this.addHandler(object : AwareRPCHandler<Request, Response> {
        override fun handle(request: Request, context: AwareRPCContext<Response>) {
            handler(request, context)
        }
    })
}