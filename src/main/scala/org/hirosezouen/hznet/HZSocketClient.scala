/*
 * Copyright (c) 2013, Hidekatsu Hirose
 * Copyright (c) 2013, Hirose-Zouen
 * This file is subject to the terms and conditions defined in
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package org.hirosezouen.hznet

import scala.actors._
import scala.actors.Actor._

// for migration from Scala Actor to Akka Actor
import scala.concurrent.duration._
import scala.actors.migration.pattern.ask
import scala.actors.migration._
import scala.concurrent._

import org.hirosezouen.hzutil._
import HZActor._
import HZLog._

case class HZSocketClient(hzSoConf: HZSoClientConf)
{
    implicit val logger = getLogger(this.getClass.getName)
    log_debug("HZSocketClient(%s)".format(hzSoConf))

    import HZSocketControler.{logger => _, _}
    import hzSoConf._

    def startSocketClientActor(staticDataBuilder: SocketIOStaticDataBuilder, parent: ActorRef, linkParent: Boolean)
                              (nextReceive: NextReceiver): ActorRef =
    {
        log_debug("startSocketClientActor")

        ActorDSL.actor(new ActWithStash {
            private implicit val actorName = ActorName("SocketClient")
            private var so_desc: HZSocketDescription = _
            private var ioActor: ActorRef = _
            private var actorSet = Set.empty[ActorRef]
            private var originReason: AnyRef = _
            private var loopfunc: () => Unit = _

            def receive = {case _ => }: PartialFunction[Any,Unit]

            private def stopClient1(reason: AnyRef, stopedActor: ActorRef = null) {
                log_hzso_actor_trace("stopClient1(%s,%s)".format(reason,stopedActor))
                if(reason != null) originReason = reason
                if(stopedActor != null) actorSet -= stopedActor
                if(stopedActor == ioActor) {
                    parent ! HZIOStop(so_desc,reason,stopedActor,ioActor,self)
                    ioActor = null
                }
                if(actorSet.isEmpty) {
                    log_hzso_actor_trace("actorSet.isEmpty==true")
                    exit(originReason)
                } else {
                    log_hzso_actor_trace("actorSet=%d".format(actorSet.size))
                    actorSet.foreach(_ ! HZStop())
                    loopfunc = loopExiting
                }
            }

            private def loopConnecting() {
                log_hzso_actor_trace("loopConnecting")
                react {
                    case HZStop() => {
                        log_hzso_actor_debug("loopConnecting:HZStop")
                        stopClient1(HZCommandStoped())
                    }
                    case HZStopWithReason(reason) => {
                        log_hzso_actor_debug("loopConnecting:HZStopWithReason(%s)".format(reason))
                        stopClient1(HZCommandStopedWithReason(reason))
                    }
                    case Exit(stopedActor: ActorRef, reason) => {
                        reason match {
                            case HZEstablished(so,connActor: ActorRef) => {
                                log_hzso_actor_debug("loopConnecting:1:Exit(%s,HZEstablished(%s))".format(stopedActor,so))
                                so.setSoTimeout(hzSoConf.recvTimeout)
                                actorSet -= connActor 
                                ioActor = startSocketIOActor(so, staticDataBuilder, self)(nextReceive)
                                actorSet += ioActor
                                so_desc = HZSocketDescription(so)
                                parent ! HZIOStart(so_desc, ioActor, self)
                                loopfunc = loopRunning
                            }
                            case _ :HZActorReason => {
                                log_hzso_actor_debug("loopConnecting:2:Exit(%s,%s)".format(stopedActor,reason))
                                stopClient1(reason,stopedActor)
                            }
                            case th: Throwable => {
                                val unHandledExp = HZUnHandledException(th)
                                log_hzso_actor_debug("loopConnecting:3:Exit(%s,HZUnHandledException)".format(stopedActor),th)
                                stopClient1(unHandledExp, stopedActor)
                            }
                            case _ => {
                                val unknownReason = HZUnknownReason(reason)
                                log_hzso_actor_debug("loopConnecting:4:Exit(%s,%s)".format(stopedActor,unknownReason))
                                stopClient1(unknownReason,stopedActor)
                            }
                        }
                    }
                }
            }

            private def loopRunning() {
                log_hzso_actor_trace("loopRunning")
                react {
//                    case dataReceived @ HZDataReceived(_) => {
//                        log_debug("SocketClient:loopRunning:HZDataReceived")
//                        parent ! dataReceived
//                    }
                    case sendData @ HZDataSending(_) => {
                        log_hzso_actor_debug("loopRunning:HZDataSending")
                        actorSet.head ! sendData
                    }
                    case HZStop() => {
                        log_hzso_actor_debug("loopRunning:HZStop")
                        stopClient1(HZCommandStoped())
                    }
                    case HZStopWithReason(reason) => {
                        log_hzso_actor_debug("loopConnecting:HZStopWithReason(%s)".format(reason))
                        stopClient1(HZCommandStopedWithReason(reason))
                    }
                    case Exit(stopedActor: ActorRef, reason) => {
                        reason match {
                            case _ :HZActorReason => {
                                log_hzso_actor_debug("loopRunning:1:Exit(%s,%s)".format(stopedActor,reason))
                                stopClient1(reason,stopedActor)
                            }
                            case th: Throwable => {
                                val unHandledExp = HZUnHandledException(th)
                                log_hzso_actor_debug("loopRunning:2:Exit(%s,HZUnHandledException)".format(stopedActor),th)
                                stopClient1(unHandledExp,stopedActor)
                            }
                            case _  => {
                                val unknownReason = HZUnknownReason(reason)
                                log_hzso_actor_debug("loopRunning:3:Exit(%s,%s)".format(stopedActor,unknownReason))
                                stopClient1(unknownReason,stopedActor)
                            }
                        }
                    }
                    case x => {
                        log_hzso_actor_debug("loopRunning:%s".format(x))
                    }
                }
            }

            private def loopExiting() {
                log_trace("SocketClient:loopExiting")
                receive[Unit] {
                    case Exit(stopedActor: ActorRef, reason) => {
                        reason match {
                            case _ :HZActorReason =>
                                log_hzso_actor_debug("loopExiting:1:Exit(%s,%s)".format(stopedActor,reason))
                            case th: Throwable =>
                                log_hzso_actor_debug("loopExiting:2:Exit(%s,HZUnHandledException)".format(stopedActor),th)
                            case _  =>
                                log_hzso_actor_debug("loopExiting:3:Exit(%s,%s)".format(stopedActor,HZUnknownReason(reason)))
                        }
                        actorSet -= stopedActor
                        if(actorSet.isEmpty) exit(originReason)
                    }
                    case x => log_hzso_actor_debug("loopExiting:%s".format(x))
                }
            }

            override def preStart() {
                log_hzso_actor_debug()

                if(linkParent) {
                    log_hzso_actor_debug("link(%s)".format(parent))
                    link(parent)
                } else {
                    log_hzso_actor_debug("no link to parent")
                }
//                self.trapExit = true

                actorSet += startConnectorActor(hzSoConf.endPoint, hzSoConf.localSocketAddressOpt, hzSoConf.connTimeout, self)
                loopfunc = loopConnecting 
            }

            override def act {
                /*
                 * main loop
                 */
                loop {
                    loopfunc()
                }
            }
        })
    }
}

object HZSocketClient {
    implicit val logger = getLogger(this.getClass.getName)

    import HZSocketControler.{logger => _, _}

    def startSocketClient(hzSoConf: HZSoClientConf,
                          staticDataBuilder: SocketIOStaticDataBuilder,
                          parent: ActorRef,
                          linkParent: Boolean = true)
                         (nextBody: NextReceiver): ActorRef
    = {
        log_debug("startSocketClient(%s,%s,%s,%s)".format(hzSoConf,staticDataBuilder,parent,linkParent))
        HZSocketClient(hzSoConf).startSocketClientActor(staticDataBuilder, parent, linkParent)(nextBody)
    }
}

