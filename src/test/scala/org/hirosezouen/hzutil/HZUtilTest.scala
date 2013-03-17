package org.hirosezouen.hzutil

import java.io.ByteArrayOutputStream

import org.scalatest.FunSuite

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent

import ch.qos.logback.core.OutputStreamAppender

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.hirosezouen.hzutil.HZLog._

class HZUtilTest extends FunSuite {

    case class TestLogger(name: String, level: Level) {

        val byteOutStream = new ByteArrayOutputStream
        val logger = getLogger("test")   

        {
            val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).asInstanceOf[ch.qos.logback.classic.Logger]
            val loggerContext = rootLogger.getLoggerContext()
            loggerContext.reset 

            val encoder = new PatternLayoutEncoder();
            encoder.setContext(loggerContext)
            encoder.setPattern("%-5level - %message%n")
            encoder.start()

            val appender = new OutputStreamAppender[ILoggingEvent]
            appender.setContext(loggerContext)
            appender.setEncoder(encoder)
            appender.setOutputStream(byteOutStream)
            appender.start

//            rootLogger.addAppender(appender)
//            rootLogger.setLevel(level)

            val logbackLogger = logger.asInstanceOf[ch.qos.logback.classic.Logger]
            logbackLogger.addAppender(appender)
            logbackLogger.setLevel(level)
        }

        def getLog: String = new String(byteOutStream.toByteArray)
    }

    test("HZLog.log_info(msg).01") {
        val test_logger = TestLogger("HZLog.log_info(msg).01", Level.INFO)
        implicit val logger = test_logger.logger

        log_info("this is " + "test log_info(msg)" + " 01")
//        println("'%s'".format(test_logger.getLog))
//        test_logger.byteOutStream.toByteArray.foreach(printf("%02X",_))
        expectResult("INFO  - this is test log_info(msg) 01\r\n")(test_logger.getLog)
    }

    test("HZLog.log_debug(msg).01") {
        val test_logger = TestLogger("HZLog.log_debug(msg).01", Level.INFO)
        implicit val logger = test_logger.logger

        var debug_count = 0
        def f(): String = {
            debug_count += 1
            debug_count.toString
        }

        log_debug("this is " + "test log_debug(" + f() +") 01")
        expectResult("")(test_logger.getLog)
        expectResult(0)(debug_count)
    }

    test("HZLog.l_t(msg).01") {
        val test_logger = TestLogger("HZLog.l_t(msg).01", Level.INFO)
        implicit val logger = test_logger.logger

        l_t("this is " + "test l_t(msg) 01")
        expectResult("")(test_logger.getLog)
    }

    test("HZLog.l_t(msg).02") {
        val test_logger = TestLogger("HZLog.l_t(msg).02", Level.TRACE)
        implicit val logger = test_logger.logger

        val ste = new Throwable().getStackTrace().apply(0)
        l_t("this is " + "test l_t(msg) 02")

//        printf("'%d'%n",ste.getLineNumber)
//        printf("'%s'%n",test_logger.getLog)
        assert(test_logger.getLog.dropRight(2).matches("TRACE - .*:%d:this is test l_t\\(msg\\) 02".format(ste.getLineNumber+1)))
    }

    test("HZProperty.01") {
        val testdata = sys.props("user.dir") + "/src/test/resources/test_data/hzProperty_test.properties"
        val prop = HZProperty(testdata)
        prop.loadProperty()

        expectResult(prop("TestKey.a"))(Some("123"))
        expectResult(prop("TestKey.b"))(Some("456"))
        expectResult(prop("TestKey.c.d"))(Some("abc"))
        expectResult(prop("TestKey.c.e"))(Some("def"))
        expectResult(prop("TestKey.c.f.g"))(Some("true"))
        expectResult(prop("TestKey.c.f.h"))(Some("false"))
        expectResult(prop("TestKey.c.f.i"))(Some(""))

        assert(prop.exists("TestKey.a"))
        assert(prop.exists("TestKey.b"))
        assert(prop.exists("TestKey.c.d"))
        assert(prop.exists("TestKey.c.e"))
        assert(prop.exists("TestKey.c.f.g"))
        assert(prop.exists("TestKey.c.f.h"))
        assert(prop.exists("TestKey.c.f.i"))
        assert(!prop.exists("TestKey.x.x.x"))

        assert(prop.nonEmpty("TestKey.a"))
        assert(prop.nonEmpty("TestKey.b"))
        assert(prop.nonEmpty("TestKey.c.d"))
        assert(prop.nonEmpty("TestKey.c.e"))
        assert(prop.nonEmpty("TestKey.c.f.g"))
        assert(prop.nonEmpty("TestKey.c.f.h"))
        assert(prop.isEmpty("TestKey.c.f.i"))
        assert(prop.isEmpty("TestKey.x.x.x"))
    }

    test("HZConfig.01") {
        import HZConfig._

        object TestKey extends Key("TestKey",RootKey) {

            val a: Key[Int] = leaf("a")
            val b: Key[Int] = leaf("b")
            val c = new C("c", this)
            class C(cn: String, cp: Key[_]) extends Key(cn,cp) {
                val d: Key[String] = leaf("d")
                val e: Key[String] = leaf("e")
                val f = new F("f",this)
                class F(fn: String, fp: Key[_]) extends Key(fn,fp) {
                    val g = leaf[Boolean]("g")
                    val h = leaf[Boolean]("h")
                    val i = leaf[String]("i")
                }
            }
        }

        val testdata = sys.props("user.dir") + "/src/test/resources/test_data/hzProperty_test.properties"
        implicit val conf = HZConfig(testdata)
        conf.loadProperty()
        expectResult(TestKey.a.optString())(Some("123"))
        expectResult(TestKey.b.optString())(Some("456"))
        expectResult(TestKey.c.d.optString())(Some("abc"))
        expectResult(TestKey.c.e.optString())(Some("def"))
        expectResult(TestKey.c.f.g.optString())(Some("true"))
        expectResult(TestKey.c.f.h.optString())(Some("false"))
        expectResult(TestKey.c.f.i.optString())(Some(""))

        expectResult(TestKey.a.optVal())(Some(123))
        expectResult(TestKey.b.optVal())(Some(456))
        expectResult(TestKey.c.d.optVal())(Some("abc"))
        expectResult(TestKey.c.e.optVal())(Some("def"))
        expectResult(TestKey.c.f.g.optVal())(Some(true))
        expectResult(TestKey.c.f.h.optVal())(Some(false))
        expectResult(TestKey.c.f.i.optVal())(Some(""))

        expectResult(TestKey.a())(123)
        expectResult(TestKey.b())(456)
        expectResult(TestKey.c.d())("abc")
        expectResult(TestKey.c.e())("def")
        expectResult(TestKey.c.f.g())(true)
        expectResult(TestKey.c.f.h())(false)
        expectResult(TestKey.c.f.i())("")

        assert(TestKey.a.nonEmpty())
        assert(TestKey.b.nonEmpty())
        assert(TestKey.c.d.nonEmpty())
        assert(TestKey.c.e.nonEmpty())
        assert(TestKey.c.f.g.nonEmpty())
        assert(TestKey.c.f.h.nonEmpty())
        assert(TestKey.c.f.i.isEmpty())

        expectResult(TestKey.paths)(List("TestKey","TestKey.a","TestKey.b","TestKey.c","TestKey.c.d","TestKey.c.e","TestKey.c.f","TestKey.c.f.g","TestKey.c.f.h","TestKey.c.f.i"))
        expectResult(TestKey.paths((l: Key[_]) => l.isLeaf))(List("TestKey.a","TestKey.b","TestKey.c.d","TestKey.c.e","TestKey.c.f.g","TestKey.c.f.h","TestKey.c.f.i"))
    }
}

