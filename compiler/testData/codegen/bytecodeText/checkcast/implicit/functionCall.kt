open class SuperBase

open class Base : SuperBase() {
    fun test(base: Base) {
        testSuper(base)
    }

    fun testSuper(base: SuperBase) {

    }
}

open class Derived : Base()

fun Base.testExt(base: Base) {
    testExtSuper(base)
}

fun SuperBase.testExtSuper(base: SuperBase) {

}


fun test() {
    Derived().test(Derived())
    Derived().testExt(Derived())
}

// 0 CHECKCAST