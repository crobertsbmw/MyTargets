/*
 * MyTargets Project Copyright (C) 2018 Florian Dreier
 *
 * This file is (c) 2021 Jez McKinley - Canford Magna Bowmen
 *
* Calculations used in this file are courtesy of Jack Atkinson - see:
 * https://www.jackatkinson.net/post/archery_handicap/
 * derived from David Lane's Handicap Calcs for Toxophilus 1979
 *
 * MyTargets is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
 *
 * MyTargets is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package de.dreier.mytargets.shared.models

import de.dreier.mytargets.shared.models.db.Round
import de.dreier.mytargets.shared.targets.models.TargetModelBase
import de.dreier.mytargets.shared.targets.scoringstyle.ScoringStyle
import java.lang.Math.exp
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

class HandicapCalculator {
    constructor(round: Round) {
        this.arrowCount = round.shotsPerEnd * round.maxEndCount!!
        this.targetModel = round.target.model
        this.scoringStyle = round.target.getScoringStyle()
        this.targetSize = round.target.diameter
        this.maxScore = round.score.totalPoints
        this.reachedScore = round.score.reachedPoints
        setTargetDistance(round.distance)
    }
    constructor()

    var reachedScore: Int = 0
        private set
    var maxScore: Int = 0
        private set
    var arrowCount: Int = 1
        private set
    lateinit var targetModel: TargetModelBase
        private set
    var targetSize: Dimension = Dimension.UNKNOWN
        private set
    lateinit var scoringStyle: ScoringStyle
        private set
    lateinit var targetDistance: Dimension
        private set
    lateinit var metricDistance: BigDecimal
        private set
    var arrowRadius = BigDecimal("0.357")
        private set

    companion object {
        @JvmStatic
        fun handicapLowerBound() : Int = 0
        @JvmStatic
        fun handicapUpperBound() : Int = 100
    }

    fun setArrowCount(arrowCount: Int) {
        this.arrowCount = arrowCount
    }

    fun setTargetDistance(distanceDimension: Dimension) {
        targetDistance = distanceDimension
        this.metricDistance = BigDecimal.valueOf(distanceDimension.convertTo(Dimension.Unit.METER).value.toDouble())
    }

    fun setScoringStyle(scoringStyle: ScoringStyle) {
        this.scoringStyle = scoringStyle
    }

    fun setTargetSize(dimension: Dimension) {
        this.targetSize = targetModel.getRealSize(dimension)
    }

    fun setTargetModel(targetModel: TargetModelBase) {
        this.targetModel = targetModel
    }

    fun setArrowRadius(arrowRadius: BigDecimal) {
        this.arrowRadius = arrowRadius
    }


    fun handicapCoefficient(handicap: Int) :Double {
        //K=1.429*10^-6 * 1.07^(handicap+4.3)
        return 1.429*(10.00.pow(-6))*1.07.pow(handicap+4.3)
    }

    fun angularDeviation(handicap: Int): BigDecimal {
        return  BigDecimal.valueOf((1.036.pow(handicap+12.9))*5*(10.0.pow(-4))*180/Math.PI)
    }

    fun dispersionFactor(handicap: Int): BigDecimal {
        //F=1 + 1.429*10^-6 * 1.07^(handicap+4.3) * distance_in_metres^2
        return BigDecimal.valueOf(1 + 1.429 * (10.0.pow(-6)) * 1.07.pow(handicap+4.3) * metricDistance.toDouble().pow(2))
    }

    fun groupRadius(handicap: Int): BigDecimal {
        //sigma=groupRadiusCm==100*distance_in_metres*(1.036^(handicap+12.9))*5*(10^-4)*Dispersion_Factor
        return BigDecimal("0.05") * metricDistance * BigDecimal.valueOf(1.036.pow(handicap+12.9)) * dispersionFactor(handicap)
    }

    fun averageArrowScoreForHandicap(handicap: Int): BigDecimal {
    //10-zone-Average_Arrow_Score=10 - (EXP(-(((1*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((6*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((7*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((8*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((9*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((10*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2))
        val groupRadiusSquared = groupRadius(handicap).pow(2)
        val zoneMap = targetModel.getZoneSizeMap(scoringStyle, targetSize)
        val bestArrowScore = BigDecimal(zoneMap.keys.max().toString())

        var zoneScoreStep = (zoneMap.keys.elementAt(0) - zoneMap.keys.elementAt(1))
        if (zoneScoreStep == 2) {
            return imperialCalc(zoneMap, groupRadiusSquared, bestArrowScore, zoneScoreStep)
        } else {
            return metricCalc(zoneMap, groupRadiusSquared, bestArrowScore)
        }
    }

    private fun metricCalc(zoneMap: Map<Int, BigDecimal>, groupRadiusSquared: BigDecimal, bestArrowScore: BigDecimal): BigDecimal {
        var exponentTotals = BigDecimal(0)
        for ((index, radius) in zoneMap.iterator()) {
            var zoneRadiusSquared = (radius + arrowRadius).pow(2)
            exponentTotals += BigDecimal.valueOf(exp(-(zoneRadiusSquared / groupRadiusSquared).toDouble()))
        }
        return (bestArrowScore - exponentTotals)
    }

    private fun imperialCalc(zoneMap: Map<Int, BigDecimal>, groupRadiusSquared: BigDecimal, bestArrowScore: BigDecimal, zoneScoreStep: Int): BigDecimal {
        var exponentTotals = BigDecimal(0)
        var lastEntry = zoneMap.entries.last()
        for ((index, radius) in zoneMap.iterator()) {
            var zoneRadiusSquared = (radius + arrowRadius).pow(2)
            if (radius == lastEntry.value) {
                exponentTotals -= BigDecimal.valueOf(exp(-(zoneRadiusSquared / groupRadiusSquared).toDouble()))
            } else {
                exponentTotals += BigDecimal.valueOf(zoneScoreStep * exp(-(zoneRadiusSquared / groupRadiusSquared).toDouble()))
            }
        }
        return (bestArrowScore - exponentTotals)
    }

    fun handicapScoresList(rounded: Boolean = true): List<BigDecimal> {
        val decimalPlaces: Int = if (rounded) 0 else 2
        var handicapList = ArrayList<BigDecimal>()
        for (handicap: Int in  handicapLowerBound()..handicapUpperBound()) {
//            handicapList.add((BigDecimal(arrowCount) * averageArrowScoreForHandicap(handicap).setScale(2, RoundingMode.HALF_UP)).setScale(0, RoundingMode.UP).toInt())
            var average = averageArrowScoreForHandicap(handicap)
            var roundScore = (BigDecimal(arrowCount) * average)
            handicapList.add(roundScore.setScale(decimalPlaces, RoundingMode.HALF_UP))
        }
        return handicapList
    }

    fun getHandicapForScore(totalScore: Int): Int {
        val score = BigDecimal(totalScore.toString())
        var scoreList = handicapScoresList()
        for (handicap: Int in  handicapLowerBound()..handicapUpperBound()) {
            if (score >= scoreList.get(handicap)) {
                return  handicap
            }
        }
        return 101
    }

    fun getHandicap(): Int {
        return getHandicapForScore(this.reachedScore)
    }

//arrowDiameterCm=arrow_diameter_cm  (0.357cm) 18/64
//targetSizeCm=target_size_cm (122)
//targetDistanceMetres=distance_m (70)
//

//15y=13.716m

//SizeOfZone = 1*targetSizeCm/20.....

    //10-zone-Average_Arrow_Score=10 - (
    // EXP(-(((1*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) +
    // EXP(-(((2*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) +
    // EXP(-(((3*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) +
    // EXP(-(((4*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) +
    // EXP(-(((5*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) +
    // EXP(-(((6*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) +
    // EXP(-(((7*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) +
    // EXP(-(((8*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) +
    // EXP(-(((9*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) +
    // EXP(-(((10*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2))

//5-zone-avg-arrow=          =9 -
// 2*(
// EXP(-(((1*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) +
// EXP(-(((2*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) +
// EXP(-(((3*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) +
// EXP(-(((4*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2)) -
// EXP(-(((5*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2)

//10-zone-compound-wa-pmoth  =10 - (EXP(-(((1*targetSizeCm/40)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((6*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((7*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((8*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((9*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((10*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2))
//10-zone-Average_Arrow_Score=10 - (EXP(-(((1*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((6*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((7*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((8*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((9*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((10*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2))
//10-zone-compound-wa-pmoth  =10 - (EXP(-(((1*targetSizeCm/40)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((6*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((7*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((8*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((9*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((10*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2))
//5-zone-avg-arrow=          =9 - 2*(EXP(-(((1*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2)) - EXP(-(((5*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2)
//10-zone-trispot==10 - (EXP(-(((1*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2)) - 6* EXP(-(((5*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2)
//6-zone-fita=10 - (EXP(-(((1*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2)) - 5* EXP(-(((6*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2)
//worcester=5 - (EXP(-(((1*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2))
//wa-field=6 - EXP(-(((1*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) - (EXP(-(((2*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2))
//beiter-hit-miss=1 - EXP(-(((1*targetSizeCm/2)+arrowDiameterCm)^2)/groupRadiusCm^2)
}
