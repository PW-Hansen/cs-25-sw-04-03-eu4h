package org.sw_08.eu4h.abstract_syntax

/* When matching on a sealed interface in a switch-case, Kotlin will warn you if you forgot to match a possible case */
sealed interface Type

data object BoolT : Type

data object IntT : Type

data object StringT : Type

data object CountryT : Type

data object ProvinceT : Type

data object MissionT : Type