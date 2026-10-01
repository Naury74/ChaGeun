package com.naury.chageun.core.common.dispatcher

import javax.inject.Qualifier

enum class ChageunDispatchers { Default, IO }

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: ChageunDispatchers)
