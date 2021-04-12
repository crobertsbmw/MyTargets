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
import de.dreier.mytargets.shared.SharedApplicationInstance
import de.dreier.mytargets.shared.models.Dimension
import de.dreier.mytargets.shared.models.MultiRoundHandicapCalculator
import de.dreier.mytargets.shared.models.Score
import de.dreier.mytargets.shared.models.Target
import de.dreier.mytargets.shared.models.db.Round
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@SmallTest
@RunWith(AndroidJUnit4::class)
class MultiRoundHandicapCalculatorTest {
    private lateinit var context: Context

    @Before
    @Throws(Exception::class)
    fun setUp() {
        SharedApplicationInstance.context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun construction_single_round() {
        var longDistance = Dimension(70f, Dimension.Unit.METER)
        var longDiameter = Dimension(122f, Dimension.Unit.CENTIMETER)
        var longTarget = Target(WAFull.ID, 0, longDiameter)
        var longScore = Score(685, 720)
        var longRound = Round(0, 0, 0, 6, 12, longDistance, "70m", longTarget, longScore)

        var arrowDiameter = Dimension(0.281f, Dimension.Unit.INCH)
        var rounds = ArrayList<Round>()
        rounds.add(longRound)
        var unit = MultiRoundHandicapCalculator(rounds, arrowDiameter)

        assertThat(unit.arrowRadius, equalTo(BigDecimal("0.357")))
        assertThat(unit.totalScore, equalTo(720))
        assertThat(unit.reachedScore, equalTo(685))
        assertThat(unit.rounds.size, equalTo(1))

    }

    @Test
    fun construction_multi_round() {
        var arrowDiameter = Dimension(0.714f, Dimension.Unit.CENTIMETER)
        var longDistance = Dimension(100f, Dimension.Unit.YARDS)
        var longDiameter = Dimension(122f, Dimension.Unit.CENTIMETER)
        var longTarget = Target(WAFull.ID, 5, longDiameter)
        var longScore = Score(504, 648)
        var longRound = Round(0, 0, 0, 6, 12, longDistance, "100y", longTarget, longScore)

        var midDistance = Dimension(80f, Dimension.Unit.YARDS)
        var midDiameter = Dimension(122f, Dimension.Unit.CENTIMETER)
        var midTarget = Target(WAFull.ID, 5, midDiameter)
        var midScore = Score(336, 432)
        var midRound = Round(0, 0, 0, 6, 8, midDistance, "80y", midTarget, midScore)

        var shortDistance = Dimension(60f, Dimension.Unit.YARDS)
        var shortDiameter = Dimension(122f, Dimension.Unit.CENTIMETER)
        var shortTarget = Target(WAFull.ID, 5, shortDiameter)
        var shortScore = Score(168, 216)
        var shortRound = Round(0, 0, 0, 6, 4, shortDistance, "60y", shortTarget, shortScore)

        var rounds = ArrayList<Round>()
        rounds.add(longRound)
        rounds.add(midRound)
        rounds.add(shortRound)
        var unit = MultiRoundHandicapCalculator(rounds, arrowDiameter)

        assertThat(unit.arrowRadius, equalTo(BigDecimal("0.357")))
        assertThat(unit.totalScore, equalTo(1296))
        assertThat(unit.reachedScore, equalTo(1008))
        assertThat(unit.rounds.size, equalTo(3))

    }

    @Test
    fun handicap_from_single_metric_wa_70() {
        //        WA, R.string.wa_70,
        //        Dimension.Unit.METER, CENTIMETER,
        // first params here are targetface, scoringstyle and shotsperend, the tuples are distance, diameter and ends
        //        WAFull.ID, 0, 6, 70, 122, 12
        var longDistance = Dimension(70f, Dimension.Unit.METER)
        var longDiameter = Dimension(122f, Dimension.Unit.CENTIMETER)
        var longTarget = Target(WAFull.ID, 0, longDiameter)
        var longScore = Score(685, 720)
        var longRound = Round(0, 0, 0, 6, 12, longDistance, "70m", longTarget, longScore)

        var arrowDiameter = Dimension(0.714f, Dimension.Unit.CENTIMETER)
        var rounds = ArrayList<Round>()
        rounds.add(longRound)
        var unit = MultiRoundHandicapCalculator(rounds, arrowDiameter)

        assertThat(unit.getHandicap(), equalTo(7))

    }

    @Test
    fun handicap_from_two_sub_rounds_metric_wa_combined_25_18() {
        //        WA, R.string.wa_combined,
        //        Dimension.Unit.METER, CENTIMETER,
        // first params here are targetface, scoringstyle and shotsperend, the tuples are distance, diameter and ends
        //        WAFull.ID, 0, 3, 25, 60, 20, 18, 40, 20
        var longDistance = Dimension(25f, Dimension.Unit.METER)
        var longDiameter = Dimension(60f, Dimension.Unit.CENTIMETER)
        var longTarget = Target(WAFull.ID, 0, longDiameter)
        var longScore = Score(590, 600)
        var longRound = Round(0, 0, 0, 3, 20, longDistance, "100y", longTarget, longScore)

        var shortDistance = Dimension(18f, Dimension.Unit.METER)
        var shortDiameter = Dimension(40f, Dimension.Unit.CENTIMETER)
        var shortTarget = Target(WAFull.ID, 0, shortDiameter)
        var shortScore = Score(591, 600)
        var shortRound = Round(0, 0, 0, 3, 20, shortDistance, "60y", shortTarget, shortScore)

        var arrowDiameter = Dimension(0.714f, Dimension.Unit.CENTIMETER)
        var rounds = ArrayList<Round>()
        rounds.add(longRound)
        rounds.add(shortRound)
        var unit = MultiRoundHandicapCalculator(rounds, arrowDiameter)

        assertThat(unit.getHandicap(), equalTo(6))

        assertThat(unit.getHandicapForScore(1181), equalTo(6))
        assertThat(unit.getHandicapForScore(976), equalTo(40))
        assertThat(unit.getHandicapForScore(215), equalTo(80))

        //David Lane's calculations only rounds the total score after the individual rounds are added together
        // Testing a few boundaries
        assertThat(unit.getHandicapForScore(995), equalTo(38))
        assertThat(unit.getHandicapForScore(994), equalTo(39))
        assertThat(unit.getHandicapForScore(985), equalTo(39))
    }

    @Test
    fun handicap_from_three_sub_rounds_imperial_york() {
        // TODO: Find out if there are any hybrid rounds in existence that use different scoring per distance
        // ARCHERY_GB, R.string.york,
        // YARDS, CENTIMETER,
        // first params here are targetface, scoringstyle and shotsperend, the tuples are distance, diameter and ends
        // WAFull.ID, 5, 6, 100, 122, 12, 80, 122, 8, 60, 122, 4
        var longDistance = Dimension(100f, Dimension.Unit.YARDS)
        var longDiameter = Dimension(122f, Dimension.Unit.CENTIMETER)
        var longTarget = Target(WAFull.ID, 5, longDiameter)
        var longScore = Score(504, 648)
        var longRound = Round(0, 0, 0, 6, 12, longDistance, "100y", longTarget, longScore)

        var midDistance = Dimension(80f, Dimension.Unit.YARDS)
        var midDiameter = Dimension(122f, Dimension.Unit.CENTIMETER)
        var midTarget = Target(WAFull.ID, 5, midDiameter)
        var midScore = Score(336, 432)
        var midRound = Round(0, 0, 0, 6, 8, midDistance, "80y", midTarget, midScore)

        var shortDistance = Dimension(60f, Dimension.Unit.YARDS)
        var shortDiameter = Dimension(122f, Dimension.Unit.CENTIMETER)
        var shortTarget = Target(WAFull.ID, 5, shortDiameter)
        var shortScore = Score(168, 216)
        var shortRound = Round(0, 0, 0, 6, 4, shortDistance, "60y", shortTarget, shortScore)

        var arrowDiameter = Dimension(0.714f, Dimension.Unit.CENTIMETER)
        var rounds = ArrayList<Round>()
        rounds.add(longRound)
        rounds.add(midRound)
        rounds.add(shortRound)
        var unit = MultiRoundHandicapCalculator(rounds, arrowDiameter)

        // Score 1008
        assertThat(unit.getHandicap(), equalTo(32))

        assertThat(unit.getHandicapForScore(1015), equalTo(31))
        assertThat(unit.getHandicapForScore(1014), equalTo(32))
        assertThat(unit.getHandicapForScore(995), equalTo(33))

        //David Lane's calculations only rounds the total score after the individual rounds are added together
        //If you round the scores of rounds before adding them together, this will calc to a 33
        assertThat(unit.getHandicapForScore(996), equalTo(32))
    }

    @Test
    fun dimensionToString() {
        var unit = Dimension(22.5487f, Dimension.Unit.CENTIMETER)
        assertThat(unit.formatString(), equalTo("22.5487 cm"))

        unit = Dimension(22.54f, Dimension.Unit.INCH)
        assertThat(unit.formatString(), equalTo("22.54 in"))
    }


}

