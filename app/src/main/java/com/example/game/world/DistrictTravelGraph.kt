package com.example.game.world

import com.example.game.districts.DistrictCatalog
import com.example.game.localization.AppLanguage
import java.util.ArrayDeque

enum class TravelRouteType(val labelEn: String, val labelBn: String) {
    HIGHWAY("National Highway", "জাতীয় মহাসড়ক"),
    BRIDGE("Major River Bridge", "বৃহৎ নদী সেতু"),
    RIVER_BOAT("River Ferry / Waterway", "নৌপথ / ফেরি"),
    COASTAL_ROAD("Marine Coastal Road", "মেরিন উপকূলীয় সড়ক");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn
}

data class TravelEdge(
    val fromId: String,
    val toId: String,
    val routeType: TravelRouteType,
    val distanceKm: Int,
    val routeNameEn: String,
    val routeNameBn: String
) {
    fun routeName(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) routeNameBn else routeNameEn
}

object DistrictTravelGraph {
    private val rawEdges = listOf(
        // Central Dhaka Hub Corridors
        TravelEdge("dhaka", "gazipur", TravelRouteType.HIGHWAY, 36, "N3 Dhaka–Gazipur Highway", "এন৩ ঢাকা–গাজীপুর মহাসড়ক"),
        TravelEdge("dhaka", "narayanganj", TravelRouteType.HIGHWAY, 22, "Demra–Narayanganj Link", "ডেমরা–নারায়ণগঞ্জ সংযোগ"),
        TravelEdge("dhaka", "munshiganj", TravelRouteType.BRIDGE, 29, "Buriganga–Mawa Expressway", "বুড়িগঙ্গা–মাওয়া এক্সপ্রেসওয়ে"),
        TravelEdge("dhaka", "manikganj", TravelRouteType.HIGHWAY, 58, "N5 Aricha Highway", "এন৫ আরিচা মহাসড়ক"),
        TravelEdge("dhaka", "narsingdi", TravelRouteType.HIGHWAY, 49, "N2 Sylhet Corridor", "এন২ সিলেট করিডোর"),
        TravelEdge("dhaka", "tangail", TravelRouteType.HIGHWAY, 94, "N4 Joydebpur–Tangail Highway", "এন৪ জয়দেবপুর–টাঙ্গাইল মহাসড়ক"),
        TravelEdge("gazipur", "mymensingh", TravelRouteType.HIGHWAY, 88, "N3 Mymensingh Highway", "এন৩ ময়মনসিংহ মহাসড়ক"),
        TravelEdge("gazipur", "tangail", TravelRouteType.HIGHWAY, 68, "Kaliakoir–Tangail Link", "কালিয়াকৈর–টাঙ্গাইল সড়ক"),
        TravelEdge("gazipur", "narsingdi", TravelRouteType.HIGHWAY, 42, "Ghorashal Industrial Link", "ঘোড়াশাল শিল্প সড়ক"),
        TravelEdge("narayanganj", "munshiganj", TravelRouteType.RIVER_BOAT, 19, "Dhaleshwari Boat Ferry", "ধলেশ্বরী নৌ-ফেরি"),
        TravelEdge("narayanganj", "cumilla", TravelRouteType.BRIDGE, 82, "Meghna–Gomti Bridge N1", "মেঘনা–গোমতী সেতু এন১"),

        // South-Central Padma Bridge & River Delta
        TravelEdge("munshiganj", "shariatpur", TravelRouteType.BRIDGE, 25, "Padma Multipurpose Bridge", "পদ্মা বহুমুখী সেতু"),
        TravelEdge("munshiganj", "madaripur", TravelRouteType.BRIDGE, 44, "Padma Express Link", "পদ্মা এক্সপ্রেসওয়ে"),
        TravelEdge("manikganj", "rajbari", TravelRouteType.RIVER_BOAT, 34, "Paturia–Daulatdia Ferry", "পাটুরিয়া–দৌলতদিয়া ফেরি"),
        TravelEdge("manikganj", "faridpur", TravelRouteType.HIGHWAY, 61, "Paturia–Faridpur Highway", "পাটুরিয়া–ফরিদপুর সড়ক"),
        TravelEdge("faridpur", "rajbari", TravelRouteType.HIGHWAY, 32, "Rajbari Feeder Highway", "রাজবাড়ী ফিডার সড়ক"),
        TravelEdge("faridpur", "madaripur", TravelRouteType.HIGHWAY, 53, "Bhanga Interchange N8", "ভাঙ্গা ইন্টারচেঞ্জ এন৮"),
        TravelEdge("faridpur", "gopalganj", TravelRouteType.HIGHWAY, 70, "N805 Gopalganj Highway", "গোপালগঞ্জ মহাসড়ক"),
        TravelEdge("faridpur", "magura", TravelRouteType.HIGHWAY, 54, "Gorai Bridge Link", "গড়াই সেতু সড়ক"),
        TravelEdge("madaripur", "shariatpur", TravelRouteType.HIGHWAY, 26, "Arial Khan Bridge Link", "আড়িয়াল খাঁ সেতু"),
        TravelEdge("madaripur", "barishal", TravelRouteType.HIGHWAY, 68, "N8 Barishal Highway", "এন৮ বরিশাল মহাসড়ক"),
        TravelEdge("gopalganj", "narail", TravelRouteType.BRIDGE, 38, "Madhumati Bridge Link", "মধুমতী সেতু"),
        TravelEdge("gopalganj", "pirojpur", TravelRouteType.HIGHWAY, 55, "Tungipara Canal Route", "টুঙ্গিপাড়া খাল সড়ক"),

        // Chattogram & Cox's Bazar Coastal Axis
        TravelEdge("cumilla", "brahmanbaria", TravelRouteType.HIGHWAY, 64, "Comilla–Sylhet Road", "কুমিল্লা–সিলেট সড়ক"),
        TravelEdge("cumilla", "chandpur", TravelRouteType.HIGHWAY, 65, "Dakatia Valley Road", "ডাকাতিয়া উপত্যকা সড়ক"),
        TravelEdge("cumilla", "feni", TravelRouteType.HIGHWAY, 57, "N1 Dhaka–Chattogram Hwy", "এন১ ঢাকা–চট্টগ্রাম মহাসড়ক"),
        TravelEdge("chandpur", "lakshmipur", TravelRouteType.RIVER_BOAT, 42, "Meghna River Waterway", "মেঘনা নৌপথ"),
        TravelEdge("chandpur", "shariatpur", TravelRouteType.RIVER_BOAT, 28, "Meghna–Padma Ferry", "মেঘনা–পদ্মা ফেরি"),
        TravelEdge("feni", "noakhali", TravelRouteType.HIGHWAY, 46, "Begumganj Highway", "বেগমগঞ্জ মহাসড়ক"),
        TravelEdge("noakhali", "lakshmipur", TravelRouteType.HIGHWAY, 36, "Raipur Coastal Link", "রায়পুর উপকূল সড়ক"),
        TravelEdge("feni", "chattogram", TravelRouteType.HIGHWAY, 94, "N1 Sitakunda Corridor", "এন১ সীতাকুণ্ড করিডোর"),
        TravelEdge("feni", "khagrachhari", TravelRouteType.HIGHWAY, 85, "Ramgarh Hill Road", "রামগড় পাহাড়ি সড়ক"),
        TravelEdge("chattogram", "khagrachhari", TravelRouteType.HIGHWAY, 112, "Hathazari Hill Climb", "হাটহাজারী পাহাড়ি পথ"),
        TravelEdge("chattogram", "rangamati", TravelRouteType.HIGHWAY, 77, "Kaptai Highland Route", "কাপ্তাই উচ্চভূমি সড়ক"),
        TravelEdge("chattogram", "bandarban", TravelRouteType.HIGHWAY, 92, "Sangu Valley Highway", "সাঙ্গু উপত্যকা সড়ক"),
        TravelEdge("chattogram", "coxs_bazar", TravelRouteType.COASTAL_ROAD, 148, "N1–Marine Drive Corridor", "এন১–মেরিন ড্রাইভ করিডোর"),
        TravelEdge("bandarban", "coxs_bazar", TravelRouteType.HIGHWAY, 115, "Lama Foothill Link", "লামা পাদদেশীয় সড়ক"),

        // Sylhet Tea Highlands & Haor
        TravelEdge("narsingdi", "kishoreganj", TravelRouteType.HIGHWAY, 69, "Bhairab Basin Road", "ভৈরব বেসিন সড়ক"),
        TravelEdge("narsingdi", "brahmanbaria", TravelRouteType.BRIDGE, 58, "Bhairab Meghna Bridge", "ভৈরব মেঘনা সেতু"),
        TravelEdge("brahmanbaria", "habiganj", TravelRouteType.HIGHWAY, 63, "N2 Habiganj Highway", "এন২ হবিগঞ্জ মহাসড়ক"),
        TravelEdge("habiganj", "moulvibazar", TravelRouteType.HIGHWAY, 54, "Sreemangal Tea Route", "শ্রীমঙ্গল চা বাগান সড়ক"),
        TravelEdge("habiganj", "sylhet", TravelRouteType.HIGHWAY, 90, "N2 Sherpur Bridge Link", "এন২ শেরপুর সেতু সড়ক"),
        TravelEdge("moulvibazar", "sylhet", TravelRouteType.HIGHWAY, 62, "Fenchuganj Tea Corridor", "ফেঞ্চুগঞ্জ চা করিডোর"),
        TravelEdge("sylhet", "sunamganj", TravelRouteType.RIVER_BOAT, 68, "Surma–Haor Link", "সুরমা–হাওর নৌপথ"),
        TravelEdge("kishoreganj", "sunamganj", TravelRouteType.RIVER_BOAT, 95, "Haor Seasonal Waterway", "হাওর নৌপথ"),
        TravelEdge("kishoreganj", "netrokona", TravelRouteType.HIGHWAY, 62, "Atharabari Rural Link", "আঠারবাড়ী গ্রামীণ সড়ক"),

        // Rajshahi Plains & Jamuna Bridge
        TravelEdge("tangail", "sirajganj", TravelRouteType.BRIDGE, 52, "Bangabandhu Jamuna Bridge", "যমুনা বহুমুখী সেতু"),
        TravelEdge("sirajganj", "pabna", TravelRouteType.HIGHWAY, 76, "Hatikumrul–Pabna Hwy", "হাটিকুমরুল–পাবনা মহাসড়ক"),
        TravelEdge("sirajganj", "bogura", TravelRouteType.HIGHWAY, 72, "N5 Bogura Highway", "এন৫ বগুড়া মহাসড়ক"),
        TravelEdge("pabna", "natore", TravelRouteType.HIGHWAY, 58, "Baraigram Highway", "বড়াইগ্রাম মহাসড়ক"),
        TravelEdge("pabna", "kushtia", TravelRouteType.BRIDGE, 35, "Hardinge & Lalon Shah Bridge", "হার্ডিঞ্জ ও লালন শাহ সেতু"),
        TravelEdge("natore", "rajshahi", TravelRouteType.HIGHWAY, 46, "N6 Rajshahi Highway", "এন৬ রাজশাহী মহাসড়ক"),
        TravelEdge("rajshahi", "chapainawabganj", TravelRouteType.HIGHWAY, 49, "Mahananda Orchard Link", "মহানন্দা আমবাগান সড়ক"),
        TravelEdge("rajshahi", "naogaon", TravelRouteType.HIGHWAY, 76, "Atrai Embankment Road", "আত্রাই বাঁধ সড়ক"),
        TravelEdge("bogura", "naogaon", TravelRouteType.HIGHWAY, 53, "Santahar Corridor", "সান্তাহার করিডোর"),
        TravelEdge("bogura", "joypurhat", TravelRouteType.HIGHWAY, 55, "Akkelpur Rail Road", "আক্কেলপুর রেল সড়ক"),
        TravelEdge("bogura", "gaibandha", TravelRouteType.HIGHWAY, 68, "Gobindaganj Highway", "গোবিন্দগঞ্জ মহাসড়ক"),

        // Khulna & Sundarbans Network
        TravelEdge("kushtia", "meherpur", TravelRouteType.HIGHWAY, 56, "Mirpur–Meherpur Link", "মিরপুর–মেহেরপুর সড়ক"),
        TravelEdge("kushtia", "chuadanga", TravelRouteType.HIGHWAY, 47, "Alamdanga Highway", "আলমডাঙ্গা সড়ক"),
        TravelEdge("kushtia", "jhenaidah", TravelRouteType.HIGHWAY, 48, "Shailkupa Highway N7", "শৈলকুপা মহাসড়ক"),
        TravelEdge("chuadanga", "jhenaidah", TravelRouteType.HIGHWAY, 39, "Kaliganj Junction Road", "কালীগঞ্জ জংশন সড়ক"),
        TravelEdge("jhenaidah", "magura", TravelRouteType.HIGHWAY, 28, "Nabaganga Bridge Road", "নবগঙ্গা সেতু সড়ক"),
        TravelEdge("jhenaidah", "jashore", TravelRouteType.HIGHWAY, 45, "N7 Jashore Corridor", "এন৭ যশোর করিডোর"),
        TravelEdge("jashore", "narail", TravelRouteType.HIGHWAY, 33, "Chitra River Crossing", "চিত্রা নদী ক্রসিং"),
        TravelEdge("jashore", "khulna", TravelRouteType.HIGHWAY, 61, "N7 Noapara Highway", "এন৭ নওয়াপাড়া মহাসড়ক"),
        TravelEdge("jashore", "satkhira", TravelRouteType.HIGHWAY, 64, "Navaron Frontier Hwy", "নাভারণ সীমান্ত সড়ক"),
        TravelEdge("khulna", "satkhira", TravelRouteType.HIGHWAY, 59, "Dumuriah Mangrove Road", "ডুমুরিয়া ম্যানগ্রোভ সড়ক"),
        TravelEdge("khulna", "bagerhat", TravelRouteType.BRIDGE, 38, "Khan Jahan Ali Bridge", "খান জাহান আলী সেতু"),
        TravelEdge("bagerhat", "pirojpur", TravelRouteType.RIVER_BOAT, 44, "Baleshwar River Ferry", "বলেশ্বর নদী ফেরি"),

        // Barishal Delta & Bay Coast
        TravelEdge("barishal", "jhalokati", TravelRouteType.RIVER_BOAT, 22, "Sugandha Canal Boat Route", "সুগন্ধা খাল নৌপথ"),
        TravelEdge("jhalokati", "pirojpur", TravelRouteType.HIGHWAY, 36, "Kawkhali Floating Route", "কাউখালী ভাসমান পথ"),
        TravelEdge("barishal", "patuakhali", TravelRouteType.HIGHWAY, 44, "Payra Bridge Highway", "পায়রা সেতু মহাসড়ক"),
        TravelEdge("patuakhali", "barguna", TravelRouteType.COASTAL_ROAD, 42, "Amtali Coastal Link", "আমতলী উপকূলীয় সড়ক"),
        TravelEdge("barishal", "bhola", TravelRouteType.RIVER_BOAT, 39, "Tentulia–Meghna Launch Route", "তেঁতুলিয়া–মেঘনা লঞ্চ রুট"),

        // Rangpur & Mymensingh Frontier
        TravelEdge("gaibandha", "rangpur", TravelRouteType.HIGHWAY, 59, "N5 Mithapukur Highway", "এন৫ মিঠাপুকুর মহাসড়ক"),
        TravelEdge("gaibandha", "kurigram", TravelRouteType.RIVER_BOAT, 64, "Teesta–Brahmaputra Ferry", "তিস্তা–ব্রহ্মপুত্র ফেরি"),
        TravelEdge("rangpur", "kurigram", TravelRouteType.BRIDGE, 54, "Dharla Bridge Highway", "ধরলা সেতু সড়ক"),
        TravelEdge("rangpur", "lalmonirhat", TravelRouteType.BRIDGE, 45, "Teesta Road Bridge", "তিস্তা সড়ক সেতু"),
        TravelEdge("rangpur", "nilphamari", TravelRouteType.HIGHWAY, 56, "Syedpur Highway", "সৈয়দপুর মহাসড়ক"),
        TravelEdge("rangpur", "dinajpur", TravelRouteType.HIGHWAY, 72, "Dashmile Highway", "দশমাইল মহাসড়ক"),
        TravelEdge("dinajpur", "thakurgaon", TravelRouteType.HIGHWAY, 58, "Birganj Highway", "বীরগঞ্জ মহাসড়ক"),
        TravelEdge("thakurgaon", "panchagarh", TravelRouteType.HIGHWAY, 43, "N5 Tetulia Frontier Hwy", "এন৫ তেঁতুলিয়া সীমান্ত সড়ক"),
        TravelEdge("nilphamari", "panchagarh", TravelRouteType.HIGHWAY, 68, "Debiganj Rural Link", "দেবীগঞ্জ গ্রামীণ সড়ক"),
        TravelEdge("mymensingh", "netrokona", TravelRouteType.HIGHWAY, 42, "Shambhuganj Highway", "শম্ভুগঞ্জ মহাসড়ক"),
        TravelEdge("mymensingh", "jamalpur", TravelRouteType.HIGHWAY, 58, "Brahmaputra South Link", "ব্রহ্মপুত্র দক্ষিণ সড়ক"),
        TravelEdge("mymensingh", "sherpur", TravelRouteType.HIGHWAY, 66, "Nakla Foothill Highway", "নকলা পাদদেশীয় সড়ক"),
        TravelEdge("jamalpur", "sherpur", TravelRouteType.BRIDGE, 19, "Old Brahmaputra Bridge", "পুরাতন ব্রহ্মপুত্র সেতু"),
        TravelEdge("jamalpur", "bogura", TravelRouteType.RIVER_BOAT, 54, "Bahadurabad–Sariakandi Ferry", "বাহাদুরাবাদ–সারিয়াকান্দি ফেরি")
    )

    val allEdges: List<TravelEdge> = rawEdges

    private val adjacency: Map<String, List<TravelEdge>> by lazy {
        val map = LinkedHashMap<String, MutableList<TravelEdge>>()
        DistrictCatalog.allDistricts.forEach { map[it.id] = ArrayList() }
        for (edge in rawEdges) {
            map[edge.fromId]?.add(edge)
            map[edge.toId]?.add(
                edge.copy(fromId = edge.toId, toId = edge.fromId)
            )
        }
        map
    }

    fun getConnectionsFrom(districtId: String): List<TravelEdge> =
        adjacency[districtId].orEmpty()

    fun getPlayableOrUnlockedNeighbors(districtId: String, unlockedDistrictIds: Set<String>): List<TravelEdge> =
        getConnectionsFrom(districtId).filter { it.toId in unlockedDistrictIds }

    fun canFastTravel(targetDistrictId: String, unlockedDistrictIds: Set<String>): Boolean =
        targetDistrictId in unlockedDistrictIds &&
            DistrictCatalog.allDistricts.any { it.id == targetDistrictId }

    fun findShortestRoute(fromId: String, toId: String): List<String> {
        if (fromId == toId) return listOf(fromId)
        val queue = ArrayDeque<String>()
        val parent = HashMap<String, String?>()
        queue.add(fromId)
        parent[fromId] = null

        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            for (edge in getConnectionsFrom(cur)) {
                if (edge.toId !in parent) {
                    parent[edge.toId] = cur
                    if (edge.toId == toId) {
                        val path = ArrayList<String>()
                        var trace: String? = toId
                        while (trace != null) {
                            path.add(trace)
                            trace = parent[trace]
                        }
                        return path.reversed()
                    }
                    queue.add(edge.toId)
                }
            }
        }
        return emptyList()
    }
}
