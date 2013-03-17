package org.hirosezouen.hzutil

import org.scalatest.FunSuite

import org.hirosezouen.hzutil._

class PackageObjectTest extends FunSuite {
    test("unsignedBigEndianShortBytes2Int") {
        expectResult(0x0000ABCD.toInt)(unsignedBigEndianShortBytes2Int(Array[Byte](0xAB.toByte,0xCD.toByte)))
        expectResult(0x0000FE01.toInt)(unsignedBigEndianShortBytes2Int(Array[Byte](0xFE.toByte,0x01.toByte)))
    }

    test("int2unsignedBigEndianShortBytes") {
        assert(Array[Byte](0xAB.toByte,0xCD.toByte) sameElements int2unsignedBigEndianShortBytes(0x0000ABCD.toInt))
        assert(Array[Byte](0xFE.toByte,0x01.toByte) sameElements int2unsignedBigEndianShortBytes(0x0000FE01.toInt))
    }

    test("unsignedBingEndianIntBytes2Long") {
        expectResult(0x00000000ABCDEF12L)(unsignedBingEndianIntBytes2Long(Array[Byte](0xAB.toByte,0xCD.toByte,0xEF.toByte,0x12.toByte)))
        expectResult(0x00000000FEDCBA98L)(unsignedBingEndianIntBytes2Long(Array[Byte](0xFE.toByte,0xDC.toByte,0xBA.toByte,0x98.toByte)))
    }

    test("log2unsignedBigEndianIntBytes") {
        assert(Array[Byte](0xAB.toByte,0xCD.toByte,0xEF.toByte,0x12.toByte) sameElements log2unsignedBigEndianIntBytes(0x00000000ABCDEF12L.toInt))
        assert(Array[Byte](0xFE.toByte,0xDC.toByte,0xBA.toByte,0x98.toByte) sameElements log2unsignedBigEndianIntBytes(0x00000000FEDCBA98L.toInt))
    }

    test("hexDump") {
        val e = """00000000 : 30313233343536373839414243444546 : 0123456789ABCDEF
                  |00000010 : 303132333435363738390d0a41424344 : 0123456789??ABCD
                  |00000020 : 30313233343536373839             : 0123456789
                  |""".stripMargin
        val a = Array[Byte](0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37,0x38,0x39,0x41,0x42,0x43,0x44,0x45,0x46,
                            0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37,0x38,0x39,0x0d,0x0a,0x41,0x42,0x43,0x44,
                            0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37,0x38,0x39)
//        printf(e)
//        printf(hexDump(a))
        expectResult(e)(hexDump(a))
    }

    test("string2ByteArray_1") {
        val e1 = Array[Byte](32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50,
                             51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69,
                             70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88,
                             89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106,
                             107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122,
                             123, 124, 125, 126)
       val a1 = string2ByteArray(""" !"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~""")

       assert(e1 sameElements a1)
    }

    test("string2ByteArray_2") {
       val e1 = Array(0x82, 0xA0, 0x82, 0xA2, 0x82, 0xA4, 0x82, 0xA6, 0x82, 0xA8, 0x94, 0x5C,
                      0x00, 0x01, 0x02, 0xab, 0xcd, 0xef, 0xff, 0x78, 0x7A, 0x7A, 0x61, 0x78, 0x61).map(_.toByte)
       val a1 = string2ByteArray("""あいうえお能\x00\x01\x02\xab\xcd\xef\xff\xzz\a\x\a""")
       assert(e1 sameElements a1)
    }
}

