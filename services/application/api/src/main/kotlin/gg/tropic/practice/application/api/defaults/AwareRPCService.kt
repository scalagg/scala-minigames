package gg.tropic.practice.application.api.defaults

import gg.scala.aware.Aware
import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.JsonRedisCodec
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.conversation.*
import gg.scala.aware.message.AwareMessage
import gg.tropic.practice.suffixWhenDev
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

interface AwareConversationMessage {
    val uniqueId: UUID
}

interface AwareConversationMessageResponse {
    val uniqueId: UUID
}

/**
 * Context object passed to RPC handlers, mimicking the existing RPCContext interface.
 */
interface AwareRPCContext<Response : AwareConversationMessageResponse> {
    fun reply(response: Response)
}

/**
 * Handler interface for processing RPC requests, mimicking the existing RPCHandler interface.
 */
interface AwareRPCHandler<Request : AwareConversationMessage, Response : AwareConversationMessageResponse> {
    /**
     * Handle an incoming RPC request.
     *
     * @param request The incoming request
     * @param context Context object for replying to the request
     */
    fun handle(request: Request, context: AwareRPCContext<Response>)
}

/**
 * RPC service interface, mimicking the existing RPCService interface.
 * Provides methods to register handlers and call remote procedures.
 */
interface AwareRPCService<Request : AwareConversationMessage, Response : AwareConversationMessageResponse> {
    /**
     * Register a handler for incoming RPC requests.
     */
    fun addHandler(handler: AwareRPCHandler<Request, Response>)

    /**
     * Call a remote procedure asynchronously.
     *
     * @param request The request to send
     * @return CompletableFuture that will complete with the response or fail with an exception
     */
    fun call(request: Request): CompletableFuture<Response>

    /**
     * Call a remote procedure synchronously, blocking until a response is received or timeout occurs.
     *
     * @param request The request to send
     * @return The response, or null if no response was received within the timeout period
     */
    fun callSync(request: Request): Response?
}

internal class AwareRPCServiceImpl<Request : AwareConversationMessage, Response : AwareConversationMessageResponse>(
    private val aware: Aware<AwareMessage>,
    private val serviceName: String,
    private val timeoutSeconds: Long
) : AwareRPCService<Request, Response> {
    private val handlers = mutableListOf<AwareRPCHandler<Request, Response>>()
    private var conversationFactory: ConversationFactory<Request, Response>? = null

    init {
        conversationFactory = ConversationFactoryBuilder
            .of<Request, Response>()
            .channel(serviceName)
            .timeout(timeoutSeconds, TimeUnit.SECONDS) {
            }
            .response { message ->
                val responseFuture = CompletableFuture<Response?>()
                val handler = handlers.firstOrNull()
                if (handler != null) {
                    val context = object : AwareRPCContext<Response> {
                        override fun reply(response: Response) {
                            responseFuture.complete(response)
                        }
                    }
                    
                    try {
                        handler.handle(message, context)
                        responseFuture.get(timeoutSeconds, TimeUnit.SECONDS)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } else {
                    null
                }
            }
            .receive { message, response ->
                ConversationContinuation.END
            }
            .build()
    }

    override fun addHandler(handler: AwareRPCHandler<Request, Response>) {
        handlers.add(handler)
    }

    override fun call(request: Request): CompletableFuture<Response> {
        val factory = conversationFactory
            ?: throw IllegalStateException("ConversationFactory not initialized")
        
        val future = CompletableFuture<Response>()
        
        factory.send(request).thenAccept { response ->
            if (response != null) {
                future.complete(response)
            } else {
                future.completeExceptionally(
                    java.util.concurrent.TimeoutException("RPC call timed out after ${timeoutSeconds} seconds")
                )
            }
        }.exceptionally { throwable ->
            future.completeExceptionally(throwable)
            null
        }
        
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

/**
 * Extension function on DPSRedisService to create RPC services.
 * Mimics the existing createRPCService extension function.
 */
fun <Request : AwareConversationMessage, Response : AwareConversationMessageResponse> DPSRedisService.createAwareRPCService(
    serviceName: String,
    timeoutSeconds: Long = 5L
): AwareRPCService<Request, Response> {
    val awareInstance = this.configure { this }
    return AwareRPCServiceImpl(awareInstance, serviceName, timeoutSeconds)
}

/**
 * Extension function on DPSRedisService to create RPC services.
 * Overload that takes a single serviceName parameter.
 */
fun <Request : AwareConversationMessage, Response : AwareConversationMessageResponse> DPSRedisService.createAwareRPCService(
    serviceName: String
): AwareRPCService<Request, Response> {
    return createAwareRPCService(serviceName, 5L)
}

/**
 * Lambda-based handler for RPC services.
 */
fun <Request : AwareConversationMessage, Response : AwareConversationMessageResponse> AwareRPCService<Request, Response>.addFunctionalHandler(
    handler: (Request, AwareRPCContext<Response>) -> Unit
) {
    this.addHandler(object : AwareRPCHandler<Request, Response> {
        override fun handle(request: Request, context: AwareRPCContext<Response>) {
            handler(request, context)
        }
    })
}
