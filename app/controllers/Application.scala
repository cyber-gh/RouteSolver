package controllers

import akka.actor.ActorSystem
import com.google.inject.Inject
import play.api.Configuration
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.mvc.{InjectedController, Request}
import sangria.execution.{ErrorWithResolver, ExceptionHandler, Executor, HandledException, MaxQueryDepthReachedError, QueryAnalysisError, QueryReducer}
import sangria.execution.deferred.DeferredResolver
import sangria.parser.{QueryParser, SyntaxError}
import sangria.renderer.SchemaRenderer

import scala.concurrent.Future
import scala.util.{Failure, Success}

class Application @Inject() (system: ActorSystem, config: Configuration) extends InjectedController{

}
