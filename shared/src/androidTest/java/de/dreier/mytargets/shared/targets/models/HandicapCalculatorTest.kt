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

package de.dreier.mytargets.shared.targets.models

import android.content.Context
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import de.dreier.mytargets.shared.R
import de.dreier.mytargets.shared.SharedApplicationInstance
import de.dreier.mytargets.shared.models.Dimension
import de.dreier.mytargets.shared.models.HandicapCalculator
import de.dreier.mytargets.shared.models.Score
import de.dreier.mytargets.shared.models.Target
import de.dreier.mytargets.shared.models.db.Round
import de.dreier.mytargets.shared.targets.scoringstyle.ScoringStyle
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.math.RoundingMode

@SmallTest
@RunWith(AndroidJUnit4::class)
class HandicapCalculatorTest {
    private lateinit var context: Context

    @Before
    @Throws(Exception::class)
    fun setUp() {
        SharedApplicationInstance.context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    fun assertBigDecimalEquals(expected: BigDecimal, actual: BigDecimal, scale: Int=2, roundingMode: RoundingMode=RoundingMode.HALF_UP) {
        assertEquals(expected.setScale(scale, roundingMode), actual.setScale(scale, roundingMode))
    }

    fun assertBigDecimalEquals(expected: String, actual: BigDecimal, scale: Int=2, roundingMode: RoundingMode=RoundingMode.HALF_UP) {
        assertEquals(BigDecimal(expected).setScale(scale, roundingMode), actual.setScale(scale, roundingMode))
    }

    fun assertBigDecimalEquals(expected: Int, actual: BigDecimal, scale: Int=2, roundingMode: RoundingMode=RoundingMode.HALF_UP) {
        assertEquals(BigDecimal(expected.toString()).setScale(scale, roundingMode), actual.setScale(scale, roundingMode))
    }

    @Test
    fun set_distance_in_metres() {
        val unit = HandicapCalculator()
        unit.setTargetDistance(Dimension(18f, Dimension.Unit.METER))
        assertBigDecimalEquals(BigDecimal.valueOf(18), unit.metricDistance, scale = 0)
    }

    @Test
    fun test_set_distance_in_yards_calculates_metres() {
        val unit = HandicapCalculator()
        unit.setTargetDistance(Dimension(15f, Dimension.Unit.YARDS))
        assertEquals(Dimension.Unit.YARDS, unit.targetDistance.unit)
        assertEquals(15f, unit.targetDistance.value)
        assertBigDecimalEquals(BigDecimal.valueOf(13.716), unit.metricDistance, scale = 3)
    }

    @Test
    fun calculate_handicap_coefficient_for_a_selection() {
        var unit = HandicapCalculator()
        assertEquals(0.000005273988009897727, unit.handicapCoefficient(15), 0.0000000001)
        assertEquals(0.0000286242146685357, unit.handicapCoefficient(40), 0.0000000001)
        assertEquals(0.001550159776623, unit.handicapCoefficient(99), 0.0000000001)
    }

    @Test
    fun set_handicap_calcs_and_sets_coefficient_as_well() {
        var unit = HandicapCalculator()
        assertEquals(0.000005273988009897727, unit.handicapCoefficient(15), 0.0000000001)
    }

    @Test
    fun dispersion_factor_calcs_at_18_metres() {
        var unit = HandicapCalculator()
        unit.setTargetDistance(Dimension(18f, Dimension.Unit.METER))

        assertBigDecimalEquals("1.000662691287247", unit.dispersionFactor(1))
        assertBigDecimalEquals("1.00539769534959", unit.dispersionFactor(32))
        assertBigDecimalEquals("1.15900004719565", unit.dispersionFactor(82))

    }

    @Test
    fun dispersion_factor_calcs_at_50_metres() {
        var unit = HandicapCalculator()
        unit.setTargetDistance(Dimension(50f, Dimension.Unit.METER))

        assertBigDecimalEquals("1.0062640842793422", unit.dispersionFactor(4))
        assertBigDecimalEquals("1.2260465117422445", unit.dispersionFactor(57))
        assertBigDecimalEquals("4.384923959784047", unit.dispersionFactor(97))

    }

    @Test
    fun get_scoring_zone_sizes_simple_10_zone() {
        var unit = WAFull()
        var targetSize = Dimension(122f, Dimension.Unit.CENTIMETER)
        var scoringStyle = ScoringStyle(R.string.recurve_style_x_1, true, 10, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1)

        var zoneMap = unit.getZoneSizeMap(scoringStyle, targetSize)
        assertEquals(10, zoneMap.size)

        assertEquals(BigDecimal.valueOf(6.1), zoneMap.get(10)?.stripTrailingZeros())
        assertEquals(BigDecimal.valueOf(12.2), zoneMap.get(9)?.stripTrailingZeros())
        assertEquals(BigDecimal.valueOf(36.6), zoneMap.get(5)?.stripTrailingZeros())
        assertEquals(BigDecimal.valueOf(61), zoneMap.get(1)?.stripTrailingZeros())

        assertEquals(zoneMap.entries.first().value, zoneMap.get(10))
        assertEquals(zoneMap.entries.last().value, zoneMap.get(1))
    }

    @Test
    fun get_scoring_zone_sizes_5_zone_from_wa_full_10_zone_face() {
        var unit = WAFull()
//        var distance = Dimension(70f, Dimension.Unit.METER)
        var targetSize = Dimension(122f, Dimension.Unit.CENTIMETER)
        var scoringStyle = ScoringStyle(R.string.imperial_outdoor_5_zone, false, 9, 9, 9, 7, 7, 5, 5, 3, 3, 1, 1)

        var zoneMap = unit.getZoneSizeMap(scoringStyle, targetSize)
        assertEquals(5, zoneMap.size)
        assertEquals(BigDecimal.valueOf(12.2), zoneMap.get(9)?.stripTrailingZeros())
        assertEquals(BigDecimal.valueOf(36.6), zoneMap.get(5)?.stripTrailingZeros())
        assertEquals(BigDecimal.valueOf(61), zoneMap.get(1)?.stripTrailingZeros())
    }

    @Test
    fun get_scoring_zone_sizes_from_scoringstyle_and_dimension_indexes() {
        var unit = WAFull()

        // Should be compound target 60cm
        var zoneMap = unit.getZoneSizeMapFromIndices(2, 1)
        assertEquals(10, zoneMap.size)
        assertEquals("1.500", zoneMap.get(10).toString())
        assertEquals("6.00", zoneMap.get(9).toString())
        assertEquals("18.00", zoneMap.get(5).toString())
        assertEquals("30.00", zoneMap.get(1).toString())
    }

    @Test
    fun get_angular_deviation_for_handicap() {
        var unit = HandicapCalculator()
        assertBigDecimalEquals("0.0599945120", unit.angularDeviation(8))
        assertBigDecimalEquals("0.2220355297", unit.angularDeviation(45))
        assertBigDecimalEquals("0.8217381029", unit.angularDeviation(82))
    }

    @Test
    fun get_group_radius() {
        var unit = HandicapCalculator()
        unit.setTargetDistance(Dimension(70.0f, Dimension.Unit.METER))
        assertBigDecimalEquals("7.4476726237", unit.groupRadius(8))
        unit.setTargetDistance(Dimension(40.0f, Dimension.Unit.METER))
        assertBigDecimalEquals("16.4967128591", unit.groupRadius(45))
        unit.setTargetDistance(Dimension(20.0f, Dimension.Unit.METER))
        assertBigDecimalEquals("34.3146495334", unit.groupRadius(82))
    }

    @Test
    fun get_group_radius_imperial() {
        var unit = HandicapCalculator()
        unit.setTargetDistance(Dimension(60.0f, Dimension.Unit.YARDS))
        assertBigDecimalEquals(BigDecimal("37.4806676996"), unit.groupRadius(55))
    }

    @Test
    fun get_average_arrow_for_handicap_10_zone_recurve() {
        // should we use Target instead of TargetModelBase as this has size and scorestyle bound?
        // Should be 122 target 70m
        var unit = HandicapCalculator()
        unit.setTargetModel(WAFull())
        unit.setScoringStyle(WAFull().getScoringStyle(1))

       // 122cm, 70m
        unit.setTargetSize(Dimension(122f, Dimension.Unit.CENTIMETER))
        unit.setTargetDistance(Dimension(70.0f, Dimension.Unit.METER))
        assertBigDecimalEquals(BigDecimal("8.99"), unit.averageArrowScoreForHandicap(18))

        // 80cm, 60m
        unit.setTargetSize(Dimension(80f, Dimension.Unit.CENTIMETER))
        unit.setTargetDistance(Dimension(60.0f, Dimension.Unit.METER))
        assertBigDecimalEquals(BigDecimal("4.83"), unit.averageArrowScoreForHandicap(45))

        // 40cm, 40m
        unit.setTargetSize(Dimension(40f, Dimension.Unit.CENTIMETER))
        unit.setTargetDistance(Dimension(40.0f, Dimension.Unit.METER))
        assertBigDecimalEquals(BigDecimal("0.15"), unit.averageArrowScoreForHandicap(82))
    }

    @Test
    fun get_average_arrow_for_handicap_10_zone_compound() {
        // should we use Target instead of TargetModelBase as this has size and scorestyle bound?
        // Should be 122 target 70m
        var unit = HandicapCalculator()
        unit.setTargetModel(WAFull())
        unit.setScoringStyle(WAFull().getScoringStyle(2))

        // 122cm, 70m
        unit.setTargetSize(Dimension(122f, Dimension.Unit.CENTIMETER))
        unit.setTargetDistance(Dimension(70.0f, Dimension.Unit.METER))
        assertBigDecimalEquals(BigDecimal("6.5866551614"), unit.averageArrowScoreForHandicap(41))

        // 80cm, 60m
        unit.setTargetSize(Dimension(80f, Dimension.Unit.CENTIMETER))
        unit.setTargetDistance(Dimension(60.0f, Dimension.Unit.METER))
        assertBigDecimalEquals(BigDecimal("8.8028056795"), unit.averageArrowScoreForHandicap(11))

        // 40cm, 40m
        unit.setTargetSize(Dimension(40f, Dimension.Unit.CENTIMETER))
        unit.setTargetDistance(Dimension(40.0f, Dimension.Unit.METER))
        assertBigDecimalEquals(BigDecimal("0.9617396800"), unit.averageArrowScoreForHandicap(65))
    }

    @Test
    fun get_average_arrow_for_handicap_5_zone_imperial() {
        // should we use Target instead of TargetModelBase as this has size and scorestyle bound?
        // Should be 122 target 70m
        var unit = HandicapCalculator()
        unit.setTargetModel(WAFull())
        unit.setScoringStyle(WAFull().getScoringStyle(5))

        // 122cm, 70m
        unit.setTargetSize(Dimension(122f, Dimension.Unit.CENTIMETER))
        unit.setTargetDistance(Dimension(70.0f, Dimension.Unit.METER))
        assertBigDecimalEquals(BigDecimal("6.8828612028"), unit.averageArrowScoreForHandicap(36))

        // 122cm, 80y (73.152m)
        unit.setTargetSize(Dimension(122f, Dimension.Unit.CENTIMETER))
        unit.setTargetDistance(Dimension(80.0f, Dimension.Unit.YARDS))
        assertBigDecimalEquals(BigDecimal("6.7122107117"), unit.averageArrowScoreForHandicap(36))

        // 60cm, 18m
        unit.setTargetSize(Dimension(60f, Dimension.Unit.CENTIMETER))
        unit.setTargetDistance(Dimension(18.0f, Dimension.Unit.METER))
        assertBigDecimalEquals(BigDecimal("8.9152743398"), unit.averageArrowScoreForHandicap(26))

    }

    @Test
    fun test_with_different_arrow_diameter() {

        var unit = HandicapCalculator()
        unit.setTargetModel(WAFull())
        unit.setScoringStyle(WAFull().getScoringStyle(1))
        unit.setArrowRadius(BigDecimal("0.4564"))

        // 122cm, 70m
        unit.setTargetSize(Dimension(122f, Dimension.Unit.CENTIMETER))
        unit.setTargetDistance(Dimension(70.0f, Dimension.Unit.METER))
        assertBigDecimalEquals(BigDecimal("4.9782780054"), unit.averageArrowScoreForHandicap(49))

        // 60cm, 18m
        unit.setTargetSize(Dimension(60f, Dimension.Unit.CENTIMETER))
        unit.setTargetDistance(Dimension(18.0f, Dimension.Unit.METER))
        assertBigDecimalEquals(BigDecimal("9.5680222762"), unit.averageArrowScoreForHandicap(26))
    }

    @Test
    fun test_handicap_list() {

        var unit = HandicapCalculator()
        unit.setTargetModel(WAFull())

        // 122cm, 70m, WA Metric Recurve
        unit.setScoringStyle(WAFull().getScoringStyle(1))
        unit.setTargetSize(Dimension(122f, Dimension.Unit.CENTIMETER))
        unit.setTargetDistance(Dimension(70.0f, Dimension.Unit.METER))

        unit.setArrowCount(36)
        var handicapList = unit.handicapScoresList()

        assertEquals(101, handicapList.size)

        assertBigDecimalEquals(324, handicapList.get(18))
        assertBigDecimalEquals(232, handicapList.get(42))
        assertBigDecimalEquals(34, handicapList.get(68))
    }

    @Test
    fun test_unrounded_handicap_list() {

        var unit = HandicapCalculator()
        unit.setTargetModel(WAFull())

        // 122cm, 70m, WA Metric Recurve
        unit.setScoringStyle(WAFull().getScoringStyle(1))
        unit.setTargetSize(Dimension(122f, Dimension.Unit.CENTIMETER))
        unit.setTargetDistance(Dimension(70.0f, Dimension.Unit.METER))

        unit.setArrowCount(36)
        var handicapList = unit.handicapScoresList(false)

        assertEquals(101, handicapList.size)

        assertBigDecimalEquals("323.64", handicapList.get(18))
        assertBigDecimalEquals("232.19", handicapList.get(42))
        assertBigDecimalEquals("34.40", handicapList.get(68))
    }

    @Test
    fun test_get_handicap_for_score() {
        var unit = HandicapCalculator()
        unit.setTargetModel(WAFull())

        // 122cm, 70m, WA Metric Recurve
        unit.setScoringStyle(WAFull().getScoringStyle(1))
        unit.setTargetSize(Dimension(122f, Dimension.Unit.CENTIMETER))
        unit.setTargetDistance(Dimension(70.0f, Dimension.Unit.METER))

        unit.setArrowCount(72)


        assertEquals(11, unit.getHandicapForScore(675))
        assertEquals(10, unit.getHandicapForScore(676))
        assertEquals(10, unit.getHandicapForScore(678))
        assertEquals(9, unit.getHandicapForScore(679))
        assertEquals(9, unit.getHandicapForScore(680))

    }

    @Test
    fun test_get_handicap_for_score_yards() {
        var unit = HandicapCalculator()
        unit.setTargetModel(WAFull())

        // 122cm, 70m, WA Metric Recurve
        unit.setScoringStyle(WAFull().getScoringStyle(1))
        unit.setTargetSize(Dimension(122f, Dimension.Unit.CENTIMETER))
//        unit.setTargetDistance(Dimension(45.72f, Dimension.Unit.METER))
        unit.setTargetDistance(Dimension(76.5529f, Dimension.Unit.YARDS))

        unit.setArrowCount(72)

        assertEquals(50, unit.getHandicapForScore(341))

    }


    @Test
    fun handicap_calculator_construction_from_round() {

        var distance = Dimension(70f, Dimension.Unit.METER)
        var diameter = Dimension(122f, Dimension.Unit.CENTIMETER)
        var target = Target(WAFull.ID, 1,  diameter)
        var score = Score(222, 720)
        var round = Round(0, 0, 0, 6, 6, distance, "test", target, score )

        var unit = HandicapCalculator(round)

        assertThat(unit.arrowRadius, equalTo(BigDecimal("0.357")))
        assertThat(unit.targetModel, notNullValue())
        assertThat(unit.scoringStyle.title, equalTo(WAFull().getScoringStyle(1).title))
        assertThat(unit.arrowCount, equalTo(36))
        assertThat(unit.targetSize.value, equalTo(122f))
        assertThat(unit.targetSize.unit, equalTo(Dimension.Unit.CENTIMETER))
        assertThat(unit.targetDistance.value, equalTo(70f))
        assertThat(unit.targetDistance.unit, equalTo(Dimension.Unit.METER))
        assertThat(unit.metricDistance, equalTo(BigDecimal("70.0")))
        assertThat(unit.maxScore, equalTo(720))
        assertThat(unit.reachedScore, equalTo(222))

        assertThat(unit.getHandicapForScore(round.score.reachedPoints), equalTo(44))
        assertThat(unit.getHandicap(), equalTo(44))
    }

    @Test
    fun handicap_calculator_construction_from_round_imperial() {
        // TODO: check this calc (should be score style 2?)
        var distance = Dimension(76.5529f, Dimension.Unit.YARDS)
        var diameter = Dimension(122f, Dimension.Unit.CENTIMETER)
        var target = Target(WAFull.ID, 0,  diameter)
        var score = Score(341, 999)
        var round = Round(0, 0, 0, 6, 12, distance, "test", target, score )

        var unit = HandicapCalculator(round)

        assertThat(unit.targetDistance.value, equalTo(76.5529f))
        assertThat(unit.targetDistance.unit, equalTo(Dimension.Unit.YARDS))

        assertThat(unit.getHandicapForScore(round.score.reachedPoints), equalTo(50))
    }

    @Test
    fun handicap_calculator_construction_with_different_arrow_radius() {
        // TODO: set arrow radius as dimension not literal
        var distance = Dimension(70f, Dimension.Unit.METER)
        var diameter = Dimension(122f, Dimension.Unit.CENTIMETER)
        var target = Target(WAFull.ID, 0, diameter)
        var score = Score(647, 720)
        var round = Round(0, 0, 0, 6, 12, distance, "test", target, score)

        var unit = HandicapCalculator(round)
        unit.setArrowRadius(BigDecimal("0.357"))
        // 18xx arrows (Default)
        assertThat(unit.getHandicapForScore(647), equalTo(18))

        //23xx arrows
        unit.setArrowRadius(BigDecimal("0.456"))
        assertThat(unit.getHandicapForScore(648), equalTo(18))
        assertThat(unit.getHandicapForScore(647), equalTo(19))

        //27xx arrows
        unit.setArrowRadius(BigDecimal("0.53578"))
        assertThat(unit.getHandicapForScore(640), equalTo(20))
        assertThat(unit.getHandicapForScore(636), equalTo(21))
        assertThat(unit.getHandicapForScore(632), equalTo(22))

    }

}

//sigma=groupRadiusCm==100*distance_in_metres*(1.036^(handicap+12.9))*5*(10^-4)*Dispersion_Factor
//Inverse_Angular_Deviation=1/Angular_Deviation
//
//arrowDiameterCm=arrow_diameter_cm  (0.357cm) 18/64
//targetSizeCm=target_size_cm (122)
//targetDistanceMetres=distance_m (70)
//

//15y=13.716m

//SizeOfZone = 1*targetSizeCm/20.....

//10-zone-Average_Arrow_Score=10 - (EXP(-(((1*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((6*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((7*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((8*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((9*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((10*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2))
//10-zone-compound-wa-pmoth  =10 - (EXP(-(((1*targetSizeCm/40)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((6*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((7*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((8*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((9*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((10*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2))
//5-zone-avg-arrow=          =9 - 2*(EXP(-(((1*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2)) - EXP(-(((5*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2)
//10-zone-trispot==10 - (EXP(-(((1*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2)) - 6* EXP(-(((5*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2)
//6-zone-fita=10 - (EXP(-(((1*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2)) - 5* EXP(-(((6*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2)
//worcester=5 - (EXP(-(((1*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2))
//wa-field=6 - EXP(-(((1*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) - (EXP(-(((2*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2))
//beiter-hit-miss=1 - EXP(-(((1*targetSizeCm/2)+arrowDiameterCm)^2)/groupRadiusCm^2)



//Portsmouth
//Handicap	K	F	Angular deviation (degrees)	Sigma = cm dev (Group radius)	Inverse Angular Deviation		Average Arrow Score	6 Arrows	1 Doz	2.5 Doz	3 Doz	5 Doz	6 Doz	Average Arrow Score	6 Arrows	1 Doz	2 Doz	3 Doz	4 Doz	6 Doz	Average Arrow Score	6 Arrows	1 Doz	2.5 Doz	5 Doz	Average Arrow Score	6 Arrows	Dozen	2.5 Dozen	5 Dozen	Average Arrow Score	6 Arrows	Dozen	2.5 Doz	5 Dozen	Average Arrow Score	6 Arrows	Dozen	3 Doz	6 Dozen	Average Arrow Score	6 Arrows	Dozen	2.5 Doz	5 Dozen	Average Arrow Score	3 Arrows	Average Arrow Score	3 Arrows	6 Arrows	30 Arrows
//0	1.91153596182896E-06	1.00061933765163	0.0452098932	1.42119034	22.12		10.00	60	120	300	360	600	720	9.00	54	108	216	324	432	648	10.00	60	120	300	600	9.82	59	118	295	589	9.82	59	118	295	589	10.00	60	120	360	720	5.00	30	60	150	300	6.00	18	1.00	3	6	30
//15	5.27398800989773E-06	1.00170877211521	0.0768475122	2.41836118	13.01		9.85	59	118	296	355	591	709	9.00	54	108	216	324	432	648	9.85	59	118	296	591	9.44	57	113	283	567	9.44	57	113	283	567	9.85	59	118	355	709	5.00	30	60	150	300	5.85	18	1.00	3	6	30
//37	2.33658856615937E-05	1.00757054695436	0.1673186808	5.29626575	5.98		9.05	54	109	271	326	543	651	8.52	51	102	204	307	409	613	9.04	54	109	271	543	8.83	53	106	265	530	8.83	53	106	265	530	9.05	54	109	326	651	4.76	29	57	143	286	5.33	16	1.00	3	6	30
//86	0.000643261195845	1.20841662745386	0.9466129614	35.93676766	1.06		2.23	13	27	67	80	134	160	1.99	12	24	48	72	96	143	1.22	7	15	37	73	2.22	13	27	67	133	1.21	7	15	36	73	1.53	9	18	55	110	1.25	8	15	38	75	2.23	7	0.51	2	3	15
//100	0.001658670960986	1.53740939135956	1.5531343412	75.01505375	0.64		0.60	4	7	18	22	36	43	0.54	3	6	13	19	26	39	0.30	2	4	9	18	0.60	4	7	18	36	0.30	2	4	9	18	0.38	2	5	14	28	0.34	2	4	10	21	1.34	4	0.15	0	1	5

//70m
//AGB Adult Classifications	Handicap	K	F	Angular deviation (degrees)	Sigma = cm dev (Group radius)	Inverse Angular Deviation		Average Arrow Score	6 Arrows	1 Doz	2.5 Doz	3 Doz	5 Doz	6 Doz	Average Arrow Score	6 Arrows	1 Doz	2 Doz	3 Doz	4 Doz	6 Doz	Average Arrow Score	6 Arrows	1 Doz	2.5 Doz	5 Doz	Average Arrow Score	6 Arrows	Dozen	2.5 Dozen	5 Dozen	Average Arrow Score	6 Arrows	Dozen	2.5 Doz	5 Dozen	Average Arrow Score	6 Arrows	Dozen	3 Doz	6 Dozen	Average Arrow Score	6 Arrows	Dozen	2.5 Doz	5 Dozen	Average Arrow Score	3 Arrows	Average Arrow Score	3 Arrows	6 Arrows	30 Arrows
//1	2.04534347915699E-06	1.01002218304787	0.0468374494	5.77962361	21.35		9.70	58	116	291	349	582	699	8.98	54	108	216	323	431	647	9.70	58	116	291	582	9.28	56	111	279	557	9.28	56	111	279	557	9.70	58	116	349	699	4.99	30	60	150	299	5.71	17	1.00	3	6	30
//GL-GMB	52	6.44672155170852E-05	1.31588935603372	0.2844073153	45.72308703	3.52		4.22	25	51	127	152	253	304	3.82	23	46	92	137	183	275	2.69	16	32	81	162	4.21	25	50	126	252	2.68	16	32	80	161	3.26	20	39	118	235	2.33	14	28	70	140	3.27	10	0.83	3	5	25
//LL-MB	62	0.000126816770505	1.62140217547505	0.4050776827	80.24250461	2.47		1.89	11	23	57	68	113	136	1.68	10	20	40	61	81	121	1.00	6	12	30	60	1.88	11	23	56	113	0.99	6	12	30	60	1.27	8	15	46	91	1.06	6	13	32	64	2.04	6	0.44	1	3	13
//GB-3, LB-2	71	0.000233147460024	2.14242255412004	0.5568985865	145.76614046	1.80		0.65	4	8	19	23	39	47	0.57	3	7	14	21	28	41	0.32	2	4	10	19	0.65	4	8	19	39	0.32	2	4	9	19	0.41	2	5	15	29	0.37	2	4	11	22	1.36	4	0.16	0	1	5
//99	0.001550159776623	8.59578290545124	1.4991644220	1574.38199015	0.67		0.01	0	0	0	0	0	0	0.01	0	0	0	0	0	0	0.00	0	0	0	0	0.01	0	0	0	0	0.00	0	0	0	0	0.00	0	0	0	0	0.00	0	0	0	0	1.00	3	0.00	0	0	0
