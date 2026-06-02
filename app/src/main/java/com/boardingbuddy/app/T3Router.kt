package com.boardingbuddy.app

/**
 * Delhi Indira Gandhi International Airport (DEL) — Terminal 3 routing.
 *
 * Grounded in the real T3 domestic-departures layout:
 *   • Departures is on the upper level.
 *   • Domestic concourse splits: Section C = gates 27–36, Section D = gates 37–62.
 *   • Food court and lounges sit within the domestic departures area.
 *
 * Given a gate number we pick the correct section and build a turn-by-turn
 * walking route with landmarks and rest benches. Distances are representative
 * (a production version would use DIAL's indoor map feed for exact metres).
 */

data class RouteLeg(
    val icon: String,
    val say: String,
    val instruction: String,
    val landmark: String,
    val meters: Int,
    val hasBench: Boolean
)

data class GateRoute(
    val gate: String,
    val section: String,        // "C" or "D"
    val sectionLabel: String,   // e.g. "Section C (Gates 27–36)"
    val legs: List<RouteLeg>
)

object T3Router {

    /** Extract the numeric part of a gate like "27A" -> 27, "52" -> 52. */
    private fun gateNumber(gate: String): Int? =
        Regex("\\d+").find(gate)?.value?.toIntOrNull()

    fun isDomesticGate(gate: String): Boolean {
        val n = gateNumber(gate) ?: return false
        return n in 27..62
    }

    fun route(gate: String): GateRoute {
        val n = gateNumber(gate) ?: 27
        val sectionC = n in 27..36
        val section = if (sectionC) "C" else "D"
        val sectionLabel =
            if (sectionC) "Section C (Gates 27–36)" else "Section D (Gates 37–62)"

        // Direction differs by section; the rest of the concourse walk is shared.
        val branch = if (sectionC) {
            RouteLeg(
                "↘️", "Follow signs to Section C",
                "Bear right toward gates 27–36. Section D (37–62) is the other way — you do not need it.",
                "Pillar sign: SECTION C →", 120, false
            )
        } else {
            RouteLeg(
                "↙️", "Follow signs to Section D",
                "Continue left toward gates 37–62. Section C (27–36) is the other way — you do not need it.",
                "Pillar sign: SECTION D →", 180, false
            )
        }

        val pierWalk = RouteLeg(
            "🚶", "Walk down the Section $section pier",
            "Gate numbers count up as you go. Yours is Gate $gate.",
            "Washrooms on the left", if (sectionC) 90 else 160, true
        )

        val legs = listOf(
            RouteLeg(
                "🛂", "Security check cleared",
                "Collect your tray items and walk straight ahead into the Domestic Departures concourse (upper level).",
                "Overhead sign: Gates 27–62", 80, false
            ),
            RouteLeg(
                "🛍️", "Pass the shops & food court",
                "Keep walking straight. The food court and lounges run along this main concourse.",
                "Food court on your right", 150, true
            ),
            branch,
            pierWalk,
            RouteLeg(
                "🪑", "Arrive at Gate $gate",
                "Find a seat near the boarding desk. You have made it.",
                "Gate $gate desk straight ahead", 0, true
            )
        )

        return GateRoute(gate, section, sectionLabel, legs)
    }
}
