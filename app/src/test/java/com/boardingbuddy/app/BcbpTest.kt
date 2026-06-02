package com.boardingbuddy.app

import org.junit.Assert.*
import org.junit.Test

class BcbpTest {
    // A representative single-leg BCBP string (DEL->BOM, 6E, seat 12C).
    // Field layout follows IATA Res.792. Spaces are significant.
    private val sample =
        "M1SHARMA/ATUL MR      EABCDEF DELBOM6E 2043 146Y012C 100"

    @Test fun parsesCoreFields() {
        val p = Bcbp.parse(sample)
        assertNotNull(p); p!!
        assertEquals("DEL", p.fromAirport)
        assertEquals("BOM", p.toAirport)
        assertEquals("6E", p.airline)
        assertEquals("2043", p.flightNumber)
        assertEquals("12C", p.seat)
        assertTrue(p.passengerName.contains("Atul"))
        assertTrue(p.passengerName.contains("Sharma"))
    }

    @Test fun rejectsGarbage() {
        assertNull(Bcbp.parse("hello world"))
        assertNull(Bcbp.parse(""))
    }
}

class T3RouterTest {
    @Test fun gate27IsSectionC() {
        assertEquals("C", T3Router.route("27A").section)
    }
    @Test fun gate52IsSectionD() {
        assertEquals("D", T3Router.route("52").section)
    }
    @Test fun routeAlwaysEndsAtGate() {
        val r = T3Router.route("41")
        assertTrue(r.legs.last().say.contains("41"))
    }
}
