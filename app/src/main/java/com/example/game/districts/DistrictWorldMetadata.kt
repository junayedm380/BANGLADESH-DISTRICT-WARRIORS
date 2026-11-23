package com.example.game.districts

data class DistrictMetaSpec(
    val biome: DistrictBiomeType,
    val buildStatus: DistrictBuildStatus,
    val mapX: Float,
    val mapY: Float,
    val landmarkEn: String,
    val landmarkBn: String,
    val descriptionEn: String,
    val descriptionBn: String,
    val missionTitleEn: String,
    val missionTitleBn: String,
    val missionObjectiveEn: String,
    val missionObjectiveBn: String
)

object DistrictWorldMetadata {
    val playableDistrictIds: Set<String> = setOf(
        "dhaka",
        "gazipur",
        "chattogram",
        "coxs_bazar",
        "sylhet",
        "khulna",
        "rajshahi",
        "rangpur",
        "barishal",
        "mymensingh"
    )

    private val coordinates: Map<String, Pair<Float, Float>> = mapOf(
        // Dhaka Division (13)
        "dhaka" to Pair(0.51f, 0.50f),
        "gazipur" to Pair(0.52f, 0.43f),
        "narayanganj" to Pair(0.55f, 0.52f),
        "tangail" to Pair(0.43f, 0.41f),
        "manikganj" to Pair(0.44f, 0.49f),
        "munshiganj" to Pair(0.51f, 0.56f),
        "narsingdi" to Pair(0.57f, 0.46f),
        "kishoreganj" to Pair(0.61f, 0.39f),
        "faridpur" to Pair(0.42f, 0.57f),
        "gopalganj" to Pair(0.44f, 0.65f),
        "madaripur" to Pair(0.49f, 0.62f),
        "rajbari" to Pair(0.37f, 0.53f),
        "shariatpur" to Pair(0.53f, 0.61f),

        // Chattogram Division (11)
        "chattogram" to Pair(0.79f, 0.72f),
        "coxs_bazar" to Pair(0.81f, 0.87f),
        "cumilla" to Pair(0.63f, 0.54f),
        "feni" to Pair(0.68f, 0.62f),
        "noakhali" to Pair(0.63f, 0.66f),
        "lakshmipur" to Pair(0.58f, 0.63f),
        "chandpur" to Pair(0.57f, 0.58f),
        "brahmanbaria" to Pair(0.63f, 0.46f),
        "rangamati" to Pair(0.86f, 0.69f),
        "khagrachhari" to Pair(0.82f, 0.61f),
        "bandarban" to Pair(0.86f, 0.80f),

        // Rajshahi Division (8)
        "rajshahi" to Pair(0.22f, 0.40f),
        "bogura" to Pair(0.32f, 0.31f),
        "joypurhat" to Pair(0.29f, 0.25f),
        "naogaon" to Pair(0.24f, 0.31f),
        "natore" to Pair(0.28f, 0.40f),
        "chapainawabganj" to Pair(0.15f, 0.36f),
        "pabna" to Pair(0.32f, 0.46f),
        "sirajganj" to Pair(0.38f, 0.39f),

        // Khulna Division (10)
        "khulna" to Pair(0.36f, 0.74f),
        "bagerhat" to Pair(0.41f, 0.78f),
        "satkhira" to Pair(0.30f, 0.78f),
        "jashore" to Pair(0.31f, 0.66f),
        "jhenaidah" to Pair(0.31f, 0.59f),
        "magura" to Pair(0.36f, 0.59f),
        "narail" to Pair(0.38f, 0.66f),
        "kushtia" to Pair(0.29f, 0.51f),
        "chuadanga" to Pair(0.25f, 0.56f),
        "meherpur" to Pair(0.21f, 0.53f),

        // Barishal Division (6)
        "barishal" to Pair(0.51f, 0.71f),
        "barguna" to Pair(0.48f, 0.83f),
        "bhola" to Pair(0.57f, 0.74f),
        "jhalokati" to Pair(0.47f, 0.75f),
        "patuakhali" to Pair(0.52f, 0.81f),
        "pirojpur" to Pair(0.44f, 0.74f),

        // Sylhet Division (4)
        "sylhet" to Pair(0.77f, 0.28f),
        "moulvibazar" to Pair(0.76f, 0.37f),
        "habiganj" to Pair(0.69f, 0.38f),
        "sunamganj" to Pair(0.69f, 0.27f),

        // Rangpur Division (8)
        "rangpur" to Pair(0.32f, 0.19f),
        "dinajpur" to Pair(0.23f, 0.20f),
        "kurigram" to Pair(0.40f, 0.19f),
        "gaibandha" to Pair(0.36f, 0.25f),
        "nilphamari" to Pair(0.26f, 0.14f),
        "panchagarh" to Pair(0.19f, 0.08f),
        "thakurgaon" to Pair(0.18f, 0.14f),
        "lalmonirhat" to Pair(0.35f, 0.13f),

        // Mymensingh Division (4)
        "mymensingh" to Pair(0.52f, 0.31f),
        "jamalpur" to Pair(0.44f, 0.30f),
        "netrokona" to Pair(0.60f, 0.29f),
        "sherpur" to Pair(0.47f, 0.24f)
    )

    private val customSpecs: Map<String, DistrictMetaSpec> = mapOf(
        "dhaka" to DistrictMetaSpec(
            biome = DistrictBiomeType.DHAKA_METROPOLIS,
            buildStatus = DistrictBuildStatus.PLAYABLE_STREAMED,
            mapX = 0.51f, mapY = 0.50f,
            landmarkEn = "Chawk Bazar & Buriganga Bridge",
            landmarkBn = "চকবাজার ও বুড়িগঙ্গা সেতু",
            descriptionEn = "Dense metropolis hub featuring urban asphalt intersections, bustling market stalls, Buriganga canal water zone, and tactical overpasses.",
            descriptionBn = "নগর মহাসড়ক, চকবাজার দোকানপাট, বুড়িগঙ্গা খাল এবং বহুতল ভবনসমৃদ্ধ প্রধান রাজধানী অঞ্চল।",
            missionTitleEn = "Capital Beacon Relay",
            missionTitleBn = "রাজধানী বিকন সংযোগ",
            missionObjectiveEn = "Activate the Sector Relay Beacon near Chawk Bazar Plaza.",
            missionObjectiveBn = "চকবাজার প্লাজার কাছে সেক্টর রিলে বিকন সক্রিয় করুন।"
        ),
        "gazipur" to DistrictMetaSpec(
            biome = DistrictBiomeType.INDUSTRIAL_CORRIDOR,
            buildStatus = DistrictBuildStatus.PLAYABLE_STREAMED,
            mapX = 0.52f, mapY = 0.43f,
            landmarkEn = "Bhawal Sal Forest & Factory Corridor",
            landmarkBn = "ভাওয়াল শালবন ও শিল্প করিডোর",
            descriptionEn = "Twin industrial factory yards flanked by tall Bhawal sal woodland and heavy logistics highway.",
            descriptionBn = "শিল্প কারখানা ইয়ার্ড, ভাওয়াল শালবন ও উত্তরমুখী লজিস্টিক হাইওয়ে।",
            missionTitleEn = "Freight Yard Telemetry",
            missionTitleBn = "ফ্রেইট ইয়ার্ড টেলিমেট্রি",
            missionObjectiveEn = "Inspect and secure the Northern Freight Relay Pad.",
            missionObjectiveBn = "উত্তর ফ্রেইট রিলে প্যাড পরীক্ষা ও সুরক্ষিত করুন।"
        ),
        "chattogram" to DistrictMetaSpec(
            biome = DistrictBiomeType.CHATTOGRAM_PORT_HILLS,
            buildStatus = DistrictBuildStatus.PLAYABLE_STREAMED,
            mapX = 0.79f, mapY = 0.72f,
            landmarkEn = "Karnaphuli Container Port & Batali Hills",
            landmarkBn = "কর্ণফুলী কন্টেইনার বন্দর ও বাটালি হিল",
            descriptionEn = "Multi-tiered hill ridges overlooking bright shipping container stacks, port cranes, and Karnaphuli coastal road.",
            descriptionBn = "সবুজ পাহাড়, বন্দর কন্টেইনার ইয়ার্ড এবং কর্ণফুলী উপকূলীয় সড়ক।",
            missionTitleEn = "Port Manifest Recovery",
            missionTitleBn = "বন্দর ম্যানিফেস্ট উদ্ধার",
            missionObjectiveEn = "Secure the Harbor Manifest Terminal by the container berths.",
            missionObjectiveBn = "কন্টেইনার বার্থের পাশে হারবার ম্যানিফেস্ট টার্মিনাল সুরক্ষিত করুন।"
        ),
        "coxs_bazar" to DistrictMetaSpec(
            biome = DistrictBiomeType.COXS_BAZAR_COAST,
            buildStatus = DistrictBuildStatus.PLAYABLE_STREAMED,
            mapX = 0.81f, mapY = 0.87f,
            landmarkEn = "Bay of Bengal Long Beach & Marine Drive",
            landmarkBn = "বঙ্গোপসাগর দীর্ঘ সৈকত ও মেরিন ড্রাইভ",
            descriptionEn = "Golden continuous sandy shoreline, coastal palm settlements, Marine Drive asphalt strip, and swimmable ocean surf.",
            descriptionBn = "সোনালি বালুকাবেলা, মেরিন ড্রাইভ সড়ক, উপকূলীয় বসতি ও বঙ্গোপসাগরের ঢেউ।",
            missionTitleEn = "Marine Drive Watchtower",
            missionTitleBn = "মেরিন ড্রাইভ ওয়াচটাওয়ার",
            missionObjectiveEn = "Synchronize the Coastal Watchtower Beacon along Marine Drive.",
            missionObjectiveBn = "মেরিন ড্রাইভের উপকূলীয় ওয়াচটাওয়ার বিকন সিঙ্ক করুন।"
        ),
        "sylhet" to DistrictMetaSpec(
            biome = DistrictBiomeType.SYLHET_TEA_HIGHLANDS,
            buildStatus = DistrictBuildStatus.PLAYABLE_STREAMED,
            mapX = 0.77f, mapY = 0.28f,
            landmarkEn = "Malnicherra Tea Terraces & Surma Bridge",
            landmarkBn = "মালনীছড়া চা বাগান ও সুরমা সেতু",
            descriptionEn = "Stepped emerald tea garden terraces, mist-kissed hills, Surma river channel, and stone arch bridge.",
            descriptionBn = "ধাপে ধাপে সাজানো সবুজ চা বাগান, পাহাড়ি টিলা ও সুরমা নদী।",
            missionTitleEn = "Highland Tea Ridge Signal",
            missionTitleBn = "পাহাড়ি চা বাগান সংকেত",
            missionObjectiveEn = "Calibrate the Ridge Weather Relay above the tea terraces.",
            missionObjectiveBn = "চা বাগানের উপরের রিজ ওয়েদার রিলে ক্যালিব্রেট করুন।"
        ),
        "khulna" to DistrictMetaSpec(
            biome = DistrictBiomeType.KHULNA_SUNDARBANS,
            buildStatus = DistrictBuildStatus.PLAYABLE_STREAMED,
            mapX = 0.36f, mapY = 0.74f,
            landmarkEn = "Rupsha Channel & Sundarbans Mangrove",
            landmarkBn = "রূপসা নদী ও সুন্দরবন ম্যানগ্রোভ",
            descriptionEn = "Dense Sundari mangrove trunks, tidal channels, wooden country boats, and elevated timber walkways.",
            descriptionBn = "সুন্দরবন ম্যানগ্রোভ অরণ্য, জোয়ার-ভাটার নদী, কাঠের নৌকা ও বাঁশের ঘাট।",
            missionTitleEn = "Mangrove River Beacon",
            missionTitleBn = "ম্যানগ্রোভ নদী বিকন",
            missionObjectiveEn = "Check in at the Forest Ranger Stilt Outpost.",
            missionObjectiveBn = "ফরেস্ট রেঞ্জার স্টিল্ট আউটপোস্টে রিপোর্ট করুন।"
        ),
        "rajshahi" to DistrictMetaSpec(
            biome = DistrictBiomeType.RAJSHAHI_PADMA_PLAINS,
            buildStatus = DistrictBuildStatus.PLAYABLE_STREAMED,
            mapX = 0.22f, mapY = 0.40f,
            landmarkEn = "Padma Embankment & Mango Orchards",
            landmarkBn = "পদ্মা বাঁধ ও আমবাগান",
            descriptionEn = "Broad Padma river levee, fertile agricultural patchwork fields, terracotta heritage plaza, and mango groves.",
            descriptionBn = "পদ্মা নদীর বাঁধ, সবুজ ফসলি মাঠ, ঐতিহ্যবাহী পোড়ামাটির চত্বর ও আমবাগান।",
            missionTitleEn = "Padma Levee Survey",
            missionTitleBn = "পদ্মা বাঁধ জরিপ",
            missionObjectiveEn = "Complete the Levee Floodgate Telemetry check.",
            missionObjectiveBn = "বাঁধের স্লুইস গেট টেলিমেট্রি পরীক্ষা সম্পন্ন করুন।"
        ),
        "rangpur" to DistrictMetaSpec(
            biome = DistrictBiomeType.RANGPUR_NORTHERN_AGRO,
            buildStatus = DistrictBuildStatus.PLAYABLE_STREAMED,
            mapX = 0.32f, mapY = 0.19f,
            landmarkEn = "Teesta Rural Plains & Carmichael Grounds",
            landmarkBn = "তিস্তা গ্রামীণ সমভূমি ও কারমাইকেল প্রাঙ্গণ",
            descriptionEn = "Open farmland paddies, red-brick northern village homes, rural dirt roads, and heritage clock gate.",
            descriptionBn = "বিস্তীর্ণ শস্যক্ষেত্র, লাল ইটের গ্রামীণ বসতি ও উত্তরাঞ্চলীয় গ্রামীণ সড়ক।",
            missionTitleEn = "Village Granary Escort",
            missionTitleBn = "গ্রামীণ শস্যভাণ্ডার সুরক্ষা",
            missionObjectiveEn = "Verify the Rural Supply Depot Signal at the clock square.",
            missionObjectiveBn = "ক্লক স্কয়ারে গ্রামীণ সাপ্লাই ডিপো সংকেত যাচাই করুন।"
        ),
        "barishal" to DistrictMetaSpec(
            biome = DistrictBiomeType.BARISHAL_RIVER_DELTA,
            buildStatus = DistrictBuildStatus.PLAYABLE_STREAMED,
            mapX = 0.51f, mapY = 0.71f,
            landmarkEn = "Kirtonkhola River & Floating Canal Market",
            landmarkBn = "কীর্তনখোলা নদী ও ভাসমান খাল বাজার",
            descriptionEn = "Criss-crossing canals, anchored wooden boats, timber bridges, and waterside stilt settlements.",
            descriptionBn = "অসংখ্য খাল-নদী, কাঠের নৌকা, কাঠের সেতু ও নদীপারের জনপদ।",
            missionTitleEn = "Floating Market Convoy",
            missionTitleBn = "ভাসমান বাজার কনভয়",
            missionObjectiveEn = "Log cargo arrivals at the Kirtonkhola Pier Beacon.",
            missionObjectiveBn = "কীর্তনখোলা ঘাট বিকনে কার্গো আগমন নিবন্ধন করুন।"
        ),
        "mymensingh" to DistrictMetaSpec(
            biome = DistrictBiomeType.BRAHMAPUTRA_RIVERFRONT,
            buildStatus = DistrictBuildStatus.PLAYABLE_STREAMED,
            mapX = 0.52f, mapY = 0.31f,
            landmarkEn = "Brahmaputra Riverfront & Shambhuganj Span",
            landmarkBn = "ব্রহ্মপুত্র নদতীর ও শম্ভুগঞ্জ সেতু",
            descriptionEn = "Scenic Old Brahmaputra riverbank boulevard, heritage terracotta campus hall, and arched river bridge.",
            descriptionBn = "পুরাতন ব্রহ্মপুত্র নদতীর, ঐতিহ্যবাহী শিক্ষাপ্রাঙ্গণ ও সেতু পথ।",
            missionTitleEn = "Brahmaputra Bridge Watch",
            missionTitleBn = "ব্রহ্মপুত্র সেতু প্রহরা",
            missionObjectiveEn = "Activate the Riverfront Bridge Relay Station.",
            missionObjectiveBn = "নদীতীরের সেতু রিলে স্টেশন সক্রিয় করুন।"
        )
    )

    fun metaFor(id: String, division: Division, nameEn: String, nameBn: String): DistrictMetaSpec {
        customSpecs[id]?.let { return it }
        val (mx, my) = coordinates[id] ?: Pair(0.5f, 0.5f)
        val defaultBiome = when (division) {
            Division.DHAKA -> DistrictBiomeType.DHAKA_METROPOLIS
            Division.CHATTOGRAM -> if (id in setOf("rangamati", "khagrachhari", "bandarban")) {
                DistrictBiomeType.SYLHET_TEA_HIGHLANDS
            } else {
                DistrictBiomeType.CHATTOGRAM_PORT_HILLS
            }
            Division.RAJSHAHI -> DistrictBiomeType.RAJSHAHI_PADMA_PLAINS
            Division.KHULNA -> DistrictBiomeType.KHULNA_SUNDARBANS
            Division.BARISHAL -> DistrictBiomeType.BARISHAL_RIVER_DELTA
            Division.SYLHET -> DistrictBiomeType.SYLHET_TEA_HIGHLANDS
            Division.RANGPUR -> DistrictBiomeType.RANGPUR_NORTHERN_AGRO
            Division.MYMENSINGH -> DistrictBiomeType.BRAHMAPUTRA_RIVERFRONT
        }
        return DistrictMetaSpec(
            biome = defaultBiome,
            buildStatus = DistrictBuildStatus.STAGED_FUTURE_BATCH,
            mapX = mx,
            mapY = my,
            landmarkEn = "$nameEn Regional Sector",
            landmarkBn = "$nameBn আঞ্চলিক সেক্টর",
            descriptionEn = "Planned ${division.nameEn} open-world expansion zone with mapped regional road/river routes. Staged for upcoming 3D terrain batch.",
            descriptionBn = "${division.nameBn}-এর পরিকল্পিত ওপেন-ওয়ার্ল্ড অঞ্চল। পরবর্তী ৩ডি পরিবেশ ধাপের জন্য নির্ধারিত।",
            missionTitleEn = "$nameEn Recon Relay",
            missionTitleBn = "$nameBn রিকন রিলে",
            missionObjectiveEn = "Secure regional perimeter beacon in $nameEn.",
            missionObjectiveBn = "$nameBn-এ আঞ্চলিক পেরিমিটার বিকন সুরক্ষিত করুন।"
        )
    }
}

object SequentialUnlockConfig {
    val defaultOrder: List<String> = listOf(
        // First Batch: Flagship playable regional route starting Dhaka -> Gazipur
        "dhaka", "gazipur", "narayanganj", "munshiganj", "manikganj",
        "tangail", "narsingdi", "kishoreganj", "faridpur", "gopalganj",
        "madaripur", "rajbari", "shariatpur",
        // Chattogram Corridor
        "chattogram", "coxs_bazar", "cumilla", "feni", "noakhali",
        "lakshmipur", "chandpur", "brahmanbaria", "rangamati", "khagrachhari", "bandarban",
        // Sylhet Highlands
        "sylhet", "moulvibazar", "habiganj", "sunamganj",
        // Khulna & Sundarbans
        "khulna", "bagerhat", "satkhira", "jashore", "jhenaidah",
        "magura", "narail", "kushtia", "chuadanga", "meherpur",
        // Rajshahi Plains
        "rajshahi", "bogura", "joypurhat", "naogaon", "natore",
        "chapainawabganj", "pabna", "sirajganj",
        // Barishal Delta
        "barishal", "barguna", "bhola", "jhalokati", "patuakhali", "pirojpur",
        // Rangpur North
        "rangpur", "dinajpur", "kurigram", "gaibandha", "nilphamari",
        "panchagarh", "thakurgaon", "lalmonirhat",
        // Mymensingh Frontier
        "mymensingh", "jamalpur", "netrokona", "sherpur"
    )

    fun normalizeOrder(customOrder: List<String>): List<String> {
        val validIds = DistrictCatalog.allDistricts.map { it.id }.toSet()
        val deduped = customOrder.filter { it in validIds }.distinct()
        return if (deduped.size == validIds.size) deduped else defaultOrder
    }

    fun nextDistrictIdAfter(
        districtId: String,
        order: List<String> = defaultOrder
    ): String? {
        val activeOrder = normalizeOrder(order)
        val idx = activeOrder.indexOf(districtId)
        return if (idx >= 0 && idx + 1 < activeOrder.size) activeOrder[idx + 1] else null
    }

    fun prerequisiteDistrictId(
        districtId: String,
        order: List<String> = defaultOrder
    ): String? {
        val activeOrder = normalizeOrder(order)
        val idx = activeOrder.indexOf(districtId)
        return if (idx > 0) activeOrder[idx - 1] else null
    }

    fun completionPercent(
        districtId: String,
        completedMissionDistricts: Set<String>,
        completedChallengeDistricts: Set<String>
    ): Int {
        val missionDone = districtId in completedMissionDistricts
        val challengeDone = districtId in completedChallengeDistricts
        return when {
            missionDone && challengeDone -> 100
            missionDone || challengeDone -> 50
            else -> 0
        }
    }

    fun isDistrictCompleted(
        districtId: String,
        completedMissionDistricts: Set<String>,
        completedChallengeDistricts: Set<String>
    ): Boolean = districtId in completedMissionDistricts && districtId in completedChallengeDistricts
}
