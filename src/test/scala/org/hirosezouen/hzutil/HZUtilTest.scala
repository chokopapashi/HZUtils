package org.hirosezouen.hzutil

import java.io.ByteArrayOutputStream

import java.nio.ByteBuffer

import org.scalatest.FunSuite

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent

import ch.qos.logback.core.OutputStreamAppender

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.hirosezouen.hzutil.HZLog._
import org.hirosezouen.hzutil.HZByteBufferUtil._

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

        assert(TestKey.keies((l: Key[_]) => l.isLeaf).forall(_.isInstanceOf[Key[_]]))
        expectResult(TestKey.keies((l: Key[_]) => l.isLeaf).map(_.toString))(List("TestKey.a","TestKey.b","TestKey.c.d","TestKey.c.e","TestKey.c.f.g","TestKey.c.f.h","TestKey.c.f.i"))
    }

    test("HZByteBufferUtil:sputByteToBuffer") {
        val data = new Array[Byte](6)
        implicit val buffer = ByteBuffer.wrap(data)
        putByteToBuffer(2,0xAB.toByte)

        expectResult(0xAB.toByte)(buffer.get(2))
    }

    test("HZByteBufferUtil:getByteFromBuffer") {
        val data = new Array[Byte](6)
        implicit val buffer = ByteBuffer.wrap(data)
        buffer.put(3,0xAB.toByte)
        expectResult(0xAB.toByte)(getByteFromBuffer(3))
    }

    test("HZByteBufferUtil:putBytesToBuffer") {
        val data = new Array[Byte](6)
        implicit val buffer = ByteBuffer.wrap(data)
        putBytesToBuffer(2,5,Array(0xAB,0xCD,0xEF).map(_.toByte))
    
        expectResult(0xAB.toByte)(buffer.get(2))
        expectResult(0xCD.toByte)(buffer.get(3))
        expectResult(0xEF.toByte)(buffer.get(4))
    }

    test("HZByteBufferUtil:getBytesFromBuffer") {
        val data = new Array[Byte](6)
        implicit val buffer = ByteBuffer.wrap(data)
        buffer.position(3)
        buffer.put(Array(0xAB,0xCD,0xEF).map(_.toByte))
        
        val outData = getBytesFromBuffer(3,6)
        expectResult(0xAB.toByte)(outData(0))
        expectResult(0xCD.toByte)(outData(1))
        expectResult(0xEF.toByte)(outData(2))
    }

    test("HZByteBufferUtil:replaceBuffer") {
        val data = new Array[Byte](6)
        implicit val buffer = ByteBuffer.wrap(data)

        replaceBuffer(Array(0xAB,0xCD,0xEF,0x01,0x02,0x03).map(_.toByte))

        expectResult(0xAB.toByte)(data(0))
        expectResult(0xCD.toByte)(data(1))
        expectResult(0xEF.toByte)(data(2))
        expectResult(0x01.toByte)(data(3))
        expectResult(0x02.toByte)(data(4))
        expectResult(0x03.toByte)(data(5))
    }

    test("HZByteBufferUtil:putBufferToBuffer_01") {
        val data = new Array[Byte](6)
        implicit val dBuffer = ByteBuffer.wrap(data)

        val srcData = new Array[Byte](6)
        val srcBuffer = ByteBuffer.wrap(srcData)
        srcBuffer.position(2)
        srcBuffer.put(Array(0xAB,0xCD,0xEF).map(_.toByte))
        srcBuffer.flip
        srcBuffer.position(2)

        putBufferToBuffer(2,5,srcBuffer,2,5)

//        print(s"${arrayToString(data)}%n")
        expectResult(0x00.toByte)(data(0))
        expectResult(0x00.toByte)(data(1))
        expectResult(0xAB.toByte)(data(2))
        expectResult(0xCD.toByte)(data(3))
        expectResult(0xEF.toByte)(data(4))
        expectResult(0x00.toByte)(data(5))
    }

    test("HZByteBufferUtil:putBufferToBuffer_02") {
        val data = new Array[Byte](6)
        implicit val dBuffer = ByteBuffer.wrap(data)

        val srcData = Array(0xAB,0xCD,0xEF).map(_.toByte)
        val srcBuffer = ByteBuffer.wrap(srcData)
        srcBuffer.clear

        putBufferToBuffer(2,5,srcBuffer)

//        print(s"${arrayToString(data)}%n")
        expectResult(0x00.toByte)(data(0))
        expectResult(0x00.toByte)(data(1))
        expectResult(0xAB.toByte)(data(2))
        expectResult(0xCD.toByte)(data(3))
        expectResult(0xEF.toByte)(data(4))
        expectResult(0x00.toByte)(data(5))
    }

    test("HZByteBufferUtil:getBufferFromBuffer") {
        val data =  Array(0xAB,0xCD,0xEF,0x01,0x02,0x03).map(_.toByte)
        implicit val buffer = ByteBuffer.wrap(data)

        val dBuffer = getBufferFromBuffer(2,5)
        dBuffer.clear
        expectResult(3)(dBuffer.limit)
        expectResult(3)(dBuffer.capacity)

        expectResult(0xEF.toByte)(dBuffer.get(0))
        expectResult(0x01.toByte)(dBuffer.get(1))
        expectResult(0x02.toByte)(dBuffer.get(2))
    }
}

