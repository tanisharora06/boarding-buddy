package com.boardingbuddy.app

/**
 * Parser for IATA BCBP (Bar-Coded Boarding Pass) format — the text encoded in
 * the PDF417 barcode printed on virtually every airline boarding pass.
 *
 * The format is a fixed-width "Mandatory Items" block. We read it position by
 * position. Reference: IATA Resolution 792, BCBP version 'M'.
 *
 * IMPORTANT: The barcode contains the passenger name, flight number, route,
 * seat, and date — but NOT the gate or boarding time. Those are assigned later
 * and can change, so the app asks the traveller to confirm the gate after scan.
 */

data class BoardingPass(
    val passengerName: String,
    val pnr: String,
    val fromAirport: String,   // 3-letter IATA, e.g. "DEL"
    val toAirport: String,     // 3-letter IATA, e.g. "BOM"
    val airline: String,       // e.g. "6E"
    val flightNumber: String,  // e.g. "2043"
    val julianDate: Int,       // day-of-year, e.g. 146
    val seat: String,          // e.g. "12C"
    val raw: String
)

object Bcbp {

    /**
     * Parse a raw BCBP string. Returns null if it clearly isn't a boarding pass.
     * Defensive throughout: real-world scans are messy, so we guard every slice.
     */
    fun parse(raw: String): BoardingPass? {
        val s = raw.trim()
        // A single-leg BCBP begins with 'M1' (M = format code, 1 = number of legs)
        if (s.length < 60 || s.firstOrNull() != 'M') return null

        return try {
            // Field positions per IATA spec (0-indexed substrings):
            // [0]      Format code 'M'
            // [1]      Number of legs
            // [2..21]  Passenger name (20 chars, "LAST/FIRST")
            // [22]     Electronic ticket indicator
            // [23..29] PNR / booking reference (7 chars)
            // [30..32] From airport (3)
            // [33..35] To airport (3)
            // [36..38] Operating carrier (3, often 2 letters + space)
            // [39..43] Flight number (5)
            // [44..46] Julian date (3)
            // [47]     Compartment / cabin
            // [48..51] Seat number (4)
            val name = s.slice(2..21).trim().let { formatName(it) }
            val pnr = sliceSafe(s, 23, 30).trim()
            val from = sliceSafe(s, 30, 33).trim()
            val to = sliceSafe(s, 33, 36).trim()
            val carrier = sliceSafe(s, 36, 39).trim()
            val flight = sliceSafe(s, 39, 44).trim().trimStart('0')
            val julian = sliceSafe(s, 44, 47).trim().toIntOrNull() ?: 0
            val seat = sliceSafe(s, 48, 52).trim().trimStart('0')

            // Sanity: airport codes should be 3 alpha chars
            if (from.length != 3 || to.length != 3) return null

            BoardingPass(
                passengerName = name,
                pnr = pnr,
                fromAirport = from.uppercase(),
                toAirport = to.uppercase(),
                airline = carrier.uppercase(),
                flightNumber = flight,
                julianDate = julian,
                seat = seat,
                raw = s
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun sliceSafe(s: String, start: Int, end: Int): String {
        if (start >= s.length) return ""
        return s.substring(start, minOf(end, s.length))
    }

    /** "SHARMA/ATUL MR" -> "Atul Sharma" (best-effort, friendly display). */
    private fun formatName(rawName: String): String {
        val parts = rawName.split("/")
        if (parts.size < 2) return titleCase(rawName)
        val last = titleCase(parts[0])
        // first token of the given-names field, dropping titles like MR/MRS
        val first = parts[1]
            .split(" ")
            .firstOrNull { it.uppercase() !in setOf("MR", "MRS", "MS", "DR", "MISS") }
            ?: parts[1]
        return "${titleCase(first)} $last".trim()
    }

    private fun titleCase(t: String): String =
        t.lowercase().split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
}
