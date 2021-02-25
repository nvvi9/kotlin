///*
// * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
// * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
// */
//
//package org.jetbrains.kotlin.descriptors.commonizer.cir.factory
//
//import org.jetbrains.kotlin.descriptors.commonizer.cir.*
//import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirTypeAliasExpansion.Companion.toSpecificCirType
//import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirProvided
//import org.jetbrains.kotlin.types.Variance
//
//class CirTypeAliasExpansion private constructor(
//    val parent: CirTypeAliasExpansion?,
//    val typeAliasId: CirEntityId,
//    val underlyingType: CirClassOrTypeAliasType,
//    val arguments: List<CirTypeProjection>,
//    val mapping: Map<CirProvided.TypeParameter, CirTypeProjection>,
//    val typeResolver: CirTypeResolver
//) {
//    companion object {
//        fun create(
//            parent: CirTypeAliasExpansion?,
//            typeAliasId: CirEntityId,
//            arguments: List<CirTypeProjection>,
//            typeResolver: CirTypeResolver
//        ): CirTypeAliasExpansion {
//            val typeAlias: CirProvided.TypeAlias = typeResolver.resolveClassifier(typeAliasId)
//            check(typeAlias.typeParameters.size == arguments.size) {
//                "Different number of type parameters (${typeAlias.typeParameters.size}) and arguments (${arguments.size}) for type alias $typeAliasId"
//            }
//
//            val mapping = typeAlias.typeParameters.zip(arguments).toMap()
//
//            return CirTypeAliasExpansion(
//                parent = parent,
//                typeAliasId = typeAliasId,
//                underlyingType = typeAlias.underlyingType.toSpecificCirType(typeResolver),
//                arguments = arguments,
//                mapping = mapping,
//                typeResolver = typeResolver
//            )
//        }
//
//        fun createWithFormalArguments(typeAliasId: CirEntityId, typeResolver: CirTypeResolver): CirTypeAliasExpansion {
//            val typeAlias: CirProvided.TypeAlias = typeResolver.resolveClassifier(typeAliasId)
//
//            return CirTypeAliasExpansion(
//                parent = null,
//                typeAliasId = typeAliasId,
//                underlyingType = typeAlias.underlyingType.toSpecificCirType(typeResolver),
//                arguments = emptyList(),
//                mapping = emptyMap(),
//                typeResolver = typeResolver
//            )
//        }
//
//        fun CirProvided.Type.toCirType(typeResolver: CirTypeResolver): CirSimpleType {
//            when (this) {
//                is CirProvided.ClassType -> TODO()
//                is CirProvided.TypeAliasType -> TODO()
//                is CirProvided.TypeParameterType -> CirTypeFactory.createTypeParameterType(
//                    index = typeResolver.resolveTypeParameterIndex(id),
//                    isMarkedNullable = isMarkedNullable
//                )
//            }
//
//            TODO()
//        }
//
//        inline fun <reified T : CirSimpleType> CirProvided.Type.toSpecificCirType(typeResolver: CirTypeResolver): T {
//            val type = toCirType(typeResolver)
//            check(type is T) { "$this can not be converted to ${T::class.java.simpleName}" }
//            return type
//        }
//    }
//}
//
//object CirTypeAliasExpander {
//    fun expand(expansion: CirTypeAliasExpansion): CirSimpleType =
//        expandRecursively(expansion, isNullable = false, withAbbreviatedType = true)
//
//    fun expandWithoutAbbreviation(expansion: CirTypeAliasExpansion): CirSimpleType =
//        expandRecursively(expansion, isNullable = false, withAbbreviatedType = false)
//
//    private fun expandRecursively(
//        expansion: CirTypeAliasExpansion,
//        isNullable: Boolean,
//        withAbbreviatedType: Boolean
//    ): CirSimpleType {
//        val underlyingType = expansion.typeAlias.underlyingType
//        val underlyingProjection = CirTypeProjectionImpl(Variance.INVARIANT, underlyingType)
//
//        val expandedProjection = expandTypeProjection(expansion, underlyingProjection, Variance.INVARIANT)
//        check(expandedProjection is CirTypeProjectionImpl) {
//            "Type alias expansion: result for $underlyingType is $expandedProjection, should not be a star projection"
//        }
//
//        check(expandedProjection.projectionKind == Variance.INVARIANT) {
//            "Type alias expansion: result for $underlyingType is $expandedProjection, should be invariant"
//        }
//
//        val expandedType = expandedProjection.type.asSimpleType()
//        val expandedTypeWithProperNullability = CirTypeFactory.makeNullableIfNeeded(expandedType, isNullable)
//
//        return if (withAbbreviatedType)
//            CirTypeFactory.createTypeAliasType(
//                typeAliasId = expansion.typeAliasId,
//                underlyingType = expandedTypeWithProperNullability as CirClassOrTypeAliasType,
//                arguments = expansion.arguments,
//                isMarkedNullable = isNullable
//            )
//        else
//            expandedTypeWithProperNullability
//    }
//
//    private fun expandTypeProjection(
//        expansion: CirTypeAliasExpansion,
//        typeProjection: CirTypeProjection,
//        typeParameterVariance: Variance
//    ): CirTypeProjection =
//        when (typeProjection) {
//            is CirStarTypeProjection -> typeProjection
//            is CirTypeProjectionImpl -> expandTypeProjection(expansion, typeProjection, typeParameterVariance)
//        }
//
//    private fun expandTypeProjection(
//        expansion: CirTypeAliasExpansion,
//        typeProjection: CirTypeProjectionImpl,
//        typeParameterVariance: Variance
//    ): CirTypeProjection {
//        val type = typeProjection.type.asSimpleType()
//
//        val argument = (if (type is CirTypeParameterType) expansion.arguments.getOrNull(type.index) else null)
//            ?: expandNonArgumentTypeProjection(expansion, typeProjection)
//
//        return when (argument) {
//            is CirStarTypeProjection -> argument
//            is CirTypeProjectionImpl -> {
//                val argumentType = argument.type
//
//                val resultingVariance = run {
//                    val argumentVariance = argument.projectionKind
//                    val underlyingVariance = typeProjection.projectionKind
//
//                    val substitutionVariance = when {
//                        underlyingVariance == argumentVariance -> argumentVariance
//                        underlyingVariance == Variance.INVARIANT -> argumentVariance
//                        argumentVariance == Variance.INVARIANT -> underlyingVariance
//                        else -> argumentVariance
//                    }
//
//                    when {
//                        typeParameterVariance == substitutionVariance -> substitutionVariance
//                        typeParameterVariance == Variance.INVARIANT -> substitutionVariance
//                        substitutionVariance == Variance.INVARIANT -> Variance.INVARIANT
//                        else -> substitutionVariance
//                    }
//                }
//
//                val substitutedType = CirTypeFactory.makeNullableIfNeeded(argumentType.asSimpleType(), type.isMarkedNullable)
//                CirTypeProjectionImpl(resultingVariance, substitutedType)
//            }
//        }
//    }
//
//    private fun expandNonArgumentTypeProjection(
//        expansion: CirTypeAliasExpansion,
//        typeProjection: CirTypeProjectionImpl
//    ): CirTypeProjection {
//        val type = typeProjection.type.asSimpleType()
//        if (!type.requiresTypeAliasExpansion())
//            return typeProjection
//
//        return when (type) {
//            is CirTypeParameterType -> typeProjection
//            is CirTypeAliasType -> {
//                val typeAliasId = type.classifierId
//                val typeAlias = expansion.getTypeAlias(typeAliasId)
//                check(typeAlias.typeParameters.size == type.arguments.size)
//
//                val expandedArguments = type.arguments.mapIndexed { index, argument ->
//                    expandTypeProjection(expansion, argument, typeAlias.typeParameters[index].variance)
//                }
//
//                val nestedExpansion = CirTypeAliasExpansion.create(typeAliasId, typeAlias, expandedArguments)
//                val nestedExpandedType = expandRecursively(
//                    nestedExpansion,
//                    isNullable = type.isMarkedNullable,
//                    withAbbreviatedType = false
//                ) as CirClassOrTypeAliasType
//
//                val substitutedArguments = type.getSubstitutedArguments(expansion, typeAlias)
//
//                val typeWithAbbreviation = CirTypeFactory.createTypeAliasType(
//                    typeAliasId = typeAliasId,
//                    underlyingType = nestedExpandedType,
//                    arguments = substitutedArguments,
//                    isMarkedNullable = type.isMarkedNullable
//                )
//
//                CirTypeProjectionImpl(typeProjection.projectionKind, typeWithAbbreviation)
//            }
//            is CirClassType -> {
//                val classId = type.classifierId
//                val clazz = expansion.getClass(classId)
//
//                val substitutedArguments = type.getSubstitutedArguments(expansion, clazz)
//                val substitutedType = CirTypeFactory.replaceArguments(type, substitutedArguments)
//
//                CirTypeProjectionImpl(typeProjection.projectionKind, substitutedType)
//            }
//        }
//    }
//
//    private fun CirClassOrTypeAliasType.getSubstitutedArguments(
//        expansion: CirTypeAliasExpansion,
//        classifier: CirClassifier
//    ): List<CirTypeProjection> {
//        val typeParameters = classifier.typeParameters // TODO 4: calculate additional TP for outer classes if this is inner class
//
//        return arguments.mapIndexed { index, argument ->
//            when (val projection = expandTypeProjection(expansion, argument, typeParameters[index].variance)) {
//                is CirStarTypeProjection -> projection
//                is CirTypeProjectionImpl -> CirTypeProjectionImpl(
//                    projectionKind = projection.projectionKind,
//                    type = CirTypeFactory.makeNullableIfNeeded(projection.type.asSimpleType(), argument.isMarkedNullable)
//                )
//            }
//        }
//    }
//
//    private fun CirSimpleType.requiresTypeAliasExpansion(): Boolean = when (this) {
//        is CirTypeAliasType, is CirTypeParameterType -> true
//        is CirClassType -> arguments.any { it is CirTypeProjectionImpl && (it.type as? CirSimpleType)?.requiresTypeAliasExpansion() == true }
//    }
//
//    private fun CirType.asSimpleType(): CirSimpleType =
//        when (this) {
//            is CirFlexibleType -> error("Expansion for flexible types is not supported: ${this::class.java}, $this")
//            is CirSimpleType -> this
//        }
//
//    private val CirTypeProjection.isMarkedNullable: Boolean
//        get() = ((this as? CirTypeProjectionImpl)?.type as? CirSimpleType)?.isMarkedNullable == true
//}
//
//
////class CirTypeAliasExpansion private constructor(
////    val typeAliasId: CirEntityId,
////    val typeAlias: CirTypeAlias,
////    val arguments: List<CirTypeProjection> // TODO 1: add reliable addressing of arguments (not by position, but by some signature)
////) {
////    fun getTypeAlias(typeAliasId: CirEntityId): CirTypeAlias = TODO("TODO 2: load from cache")
////    fun getClass(classId: CirEntityId): CirClass = TODO("TODO 3: load from cache")
////
////    companion object {
////        fun create(typeAliasId: CirEntityId, typeAlias: CirTypeAlias, arguments: List<CirTypeProjection>): CirTypeAliasExpansion {
////            check(typeAlias.typeParameters.size == arguments.size)
////            return CirTypeAliasExpansion(typeAliasId, typeAlias, arguments)
////        }
////
////        fun createWithFormalArguments(typeAliasId: CirEntityId, typeAlias: CirTypeAlias): CirTypeAliasExpansion =
////            CirTypeAliasExpansion(typeAliasId, typeAlias, emptyList())
////    }
////}
////
////object CirTypeAliasExpander {
////    fun expand(expansion: CirTypeAliasExpansion): CirSimpleType =
////        expandRecursively(expansion, isNullable = false, withAbbreviatedType = true)
////
////    fun expandWithoutAbbreviation(expansion: CirTypeAliasExpansion): CirSimpleType =
////        expandRecursively(expansion, isNullable = false, withAbbreviatedType = false)
////
////    private fun expandRecursively(
////        expansion: CirTypeAliasExpansion,
////        isNullable: Boolean,
////        withAbbreviatedType: Boolean
////    ): CirSimpleType {
////        val underlyingType = expansion.typeAlias.underlyingType
////        val underlyingProjection = CirTypeProjectionImpl(Variance.INVARIANT, underlyingType)
////
////        val expandedProjection = expandTypeProjection(expansion, underlyingProjection, Variance.INVARIANT)
////        check(expandedProjection is CirTypeProjectionImpl) {
////            "Type alias expansion: result for $underlyingType is $expandedProjection, should not be a star projection"
////        }
////
////        check(expandedProjection.projectionKind == Variance.INVARIANT) {
////            "Type alias expansion: result for $underlyingType is $expandedProjection, should be invariant"
////        }
////
////        val expandedType = expandedProjection.type.asSimpleType()
////        val expandedTypeWithProperNullability = CirTypeFactory.makeNullableIfNeeded(expandedType, isNullable)
////
////        return if (withAbbreviatedType)
////            CirTypeFactory.createTypeAliasType(
////                typeAliasId = expansion.typeAliasId,
////                underlyingType = expandedTypeWithProperNullability as CirClassOrTypeAliasType,
////                arguments = expansion.arguments,
////                isMarkedNullable = isNullable
////            )
////        else
////            expandedTypeWithProperNullability
////    }
////
////    private fun expandTypeProjection(
////        expansion: CirTypeAliasExpansion,
////        typeProjection: CirTypeProjection,
////        typeParameterVariance: Variance
////    ): CirTypeProjection =
////        when (typeProjection) {
////            is CirStarTypeProjection -> typeProjection
////            is CirTypeProjectionImpl -> expandTypeProjection(expansion, typeProjection, typeParameterVariance)
////        }
////
////    private fun expandTypeProjection(
////        expansion: CirTypeAliasExpansion,
////        typeProjection: CirTypeProjectionImpl,
////        typeParameterVariance: Variance
////    ): CirTypeProjection {
////        val type = typeProjection.type.asSimpleType()
////
////        val argument = (if (type is CirTypeParameterType) expansion.arguments.getOrNull(type.index) else null)
////            ?: expandNonArgumentTypeProjection(expansion, typeProjection)
////
////        return when (argument) {
////            is CirStarTypeProjection -> argument
////            is CirTypeProjectionImpl -> {
////                val argumentType = argument.type
////
////                val resultingVariance = run {
////                    val argumentVariance = argument.projectionKind
////                    val underlyingVariance = typeProjection.projectionKind
////
////                    val substitutionVariance = when {
////                        underlyingVariance == argumentVariance -> argumentVariance
////                        underlyingVariance == Variance.INVARIANT -> argumentVariance
////                        argumentVariance == Variance.INVARIANT -> underlyingVariance
////                        else -> argumentVariance
////                    }
////
////                    when {
////                        typeParameterVariance == substitutionVariance -> substitutionVariance
////                        typeParameterVariance == Variance.INVARIANT -> substitutionVariance
////                        substitutionVariance == Variance.INVARIANT -> Variance.INVARIANT
////                        else -> substitutionVariance
////                    }
////                }
////
////                val substitutedType = CirTypeFactory.makeNullableIfNeeded(argumentType.asSimpleType(), type.isMarkedNullable)
////                CirTypeProjectionImpl(resultingVariance, substitutedType)
////            }
////        }
////    }
////
////    private fun expandNonArgumentTypeProjection(
////        expansion: CirTypeAliasExpansion,
////        typeProjection: CirTypeProjectionImpl
////    ): CirTypeProjection {
////        val type = typeProjection.type.asSimpleType()
////        if (!type.requiresTypeAliasExpansion())
////            return typeProjection
////
////        return when (type) {
////            is CirTypeParameterType -> typeProjection
////            is CirTypeAliasType -> {
////                val typeAliasId = type.classifierId
////                val typeAlias = expansion.getTypeAlias(typeAliasId)
////                check(typeAlias.typeParameters.size == type.arguments.size)
////
////                val expandedArguments = type.arguments.mapIndexed { index, argument ->
////                    expandTypeProjection(expansion, argument, typeAlias.typeParameters[index].variance)
////                }
////
////                val nestedExpansion = CirTypeAliasExpansion.create(typeAliasId, typeAlias, expandedArguments)
////                val nestedExpandedType = expandRecursively(
////                    nestedExpansion,
////                    isNullable = type.isMarkedNullable,
////                    withAbbreviatedType = false
////                ) as CirClassOrTypeAliasType
////
////                val substitutedArguments = type.getSubstitutedArguments(expansion, typeAlias)
////
////                val typeWithAbbreviation = CirTypeFactory.createTypeAliasType(
////                    typeAliasId = typeAliasId,
////                    underlyingType = nestedExpandedType,
////                    arguments = substitutedArguments,
////                    isMarkedNullable = type.isMarkedNullable
////                )
////
////                CirTypeProjectionImpl(typeProjection.projectionKind, typeWithAbbreviation)
////            }
////            is CirClassType -> {
////                val classId = type.classifierId
////                val clazz = expansion.getClass(classId)
////
////                val substitutedArguments = type.getSubstitutedArguments(expansion, clazz)
////                val substitutedType = CirTypeFactory.replaceArguments(type, substitutedArguments)
////
////                CirTypeProjectionImpl(typeProjection.projectionKind, substitutedType)
////            }
////        }
////    }
////
////    private fun CirClassOrTypeAliasType.getSubstitutedArguments(
////        expansion: CirTypeAliasExpansion,
////        classifier: CirClassifier
////    ): List<CirTypeProjection> {
////        val typeParameters = classifier.typeParameters // TODO 4: calculate additional TP for outer classes if this is inner class
////
////        return arguments.mapIndexed { index, argument ->
////            when (val projection = expandTypeProjection(expansion, argument, typeParameters[index].variance)) {
////                is CirStarTypeProjection -> projection
////                is CirTypeProjectionImpl -> CirTypeProjectionImpl(
////                    projectionKind = projection.projectionKind,
////                    type = CirTypeFactory.makeNullableIfNeeded(projection.type.asSimpleType(), argument.isMarkedNullable)
////                )
////            }
////        }
////    }
////
////    private fun CirSimpleType.requiresTypeAliasExpansion(): Boolean = when (this) {
////        is CirTypeAliasType, is CirTypeParameterType -> true
////        is CirClassType -> arguments.any { it is CirTypeProjectionImpl && (it.type as? CirSimpleType)?.requiresTypeAliasExpansion() == true }
////    }
////
////    private fun CirType.asSimpleType(): CirSimpleType =
////        when (this) {
////            is CirFlexibleType -> error("Expansion for flexible types is not supported: ${this::class.java}, $this")
////            is CirSimpleType -> this
////        }
////
////    private val CirTypeProjection.isMarkedNullable: Boolean
////        get() = ((this as? CirTypeProjectionImpl)?.type as? CirSimpleType)?.isMarkedNullable == true
////}
