///***************************/
//class A<T>
//
//// type parameters: no
//// arguments:
//// 0: String
//typealias TA1 = A<String>
//
//// type parameter:
//// 0: K
//// argument:
//// 0: #0
//typealias TA2<K> = A<K>
//
///***************************/
//class B<X, Y>
//
//// type parameters:
//// 0: T
//// arguments:
//// 0: String
//// 1: #0
//typealias TA3<T> = B<String, T>

/***************************/
class C1<P0> {
    class C2<P1, P2>
}

class C3<P0> {
    inner class C4<P1, P2>
}

typealias T1<P0, P1> = C1.C2<P1, P0>
typealias T2<P0, P1> = T1<P0, P1>
typealias T3<P0, P1> = T2<P1, P0>

fun <P0, P1> test1(): T3<P0, P1> = TODO()

// T3<#0, #1> = T2<#1, #0> = T1<#1, #0> = C1.C2<#0, #1>

typealias T4<P0, P1, P2> = C3<P1>.C4<P2, P0>
typealias T5<P0, P1, P2> = T4<P2, P1, P0>
typealias T6<P0, P1, P2> = T5<P1, P2, P0>

fun <P0, P1, P2> test2(): T6<P0, P1, P2>

// T3<T, R> = T2<R, T> = T1<R, T> = C<T, R>

fun test3(): T2<String, Int> = TODO()
