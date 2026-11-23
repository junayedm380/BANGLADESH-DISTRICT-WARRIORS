package com.example.game.districts

import com.example.game.localization.AppLanguage

enum class Division(val id: String, val nameEn: String, val nameBn: String) {
    DHAKA("dhaka", "Dhaka Division", "ঢাকা বিভাগ"),
    CHATTOGRAM("chattogram", "Chattogram Division", "চট্টগ্রাম বিভাগ"),
    RAJSHAHI("rajshahi", "Rajshahi Division", "রাজশাহী বিভাগ"),
    KHULNA("khulna", "Khulna Division", "খুলনা বিভাগ"),
    BARISHAL("barishal", "Barishal Division", "বরিশাল বিভাগ"),
    SYLHET("sylhet", "Sylhet Division", "সিলেট বিভাগ"),
    RANGPUR("rangpur", "Rangpur Division", "রংপুর বিভাগ"),
    MYMENSINGH("mymensingh", "Mymensingh Division", "ময়মনসিংহ বিভাগ");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) nameBn else nameEn
}

enum class DistrictBiomeType(val labelEn: String, val labelBn: String) {
    DHAKA_METROPOLIS("Urban Roads, Markets & Bridges", "নগর সড়ক, বাজার ও সেতু"),
    INDUSTRIAL_CORRIDOR("Industrial Hub & Sal Forest", "শিল্প করিডোর ও শালবন"),
    CHATTOGRAM_PORT_HILLS("Hills, Port & Coastal Roads", "পাহাড়, বন্দর ও উপকূলীয় সড়ক"),
    COXS_BAZAR_COAST("Long Beach & Coastal Settlements", "দীর্ঘ সমুদ্রসৈকত ও উপকূলীয় বসতি"),
    SYLHET_TEA_HIGHLANDS("Tea Gardens, Green Hills & Rivers", "চা বাগান, সবুজ পাহাড় ও নদী"),
    KHULNA_SUNDARBANS("Sundarbans Mangrove, Rivers & Boats", "সুন্দরবন ম্যানগ্রোভ, নদী ও নৌকা"),
    RAJSHAHI_PADMA_PLAINS("Countryside, Padma River & Farmland", "গ্রামীণ জনপদ, পদ্মা ও কৃষিজমি"),
    RANGPUR_NORTHERN_AGRO("Villages, Farmland & Rural Roads", "গ্রাম, ফসলি জমি ও গ্রামীণ সড়ক"),
    BARISHAL_RIVER_DELTA("Rivers, Canals, Boats & Settlements", "নদী, খাল, নৌকা ও জনবসতি"),
    BRAHMAPUTRA_RIVERFRONT("Brahmaputra Bank & Heritage Bridge", "ব্রহ্মপুত্র তীর ও ঐতিহ্যবাহী সেতু");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn
}

enum class DistrictBuildStatus(val labelEn: String, val labelBn: String) {
    PLAYABLE_STREAMED("3D STREAMED READY", "৩ডি স্ট্রিমড প্রস্তুত"),
    STAGED_FUTURE_BATCH("STAGED EXPANSION", "পরবর্তী ধাপের এলাকা");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn
}

data class DistrictEntry(
    val id: String,
    val division: Division,
    val unlockOrder: Int,
    val nameEn: String,
    val nameBn: String,
    val sceneKey: String,
    val challengeId: String,
    val challengeTitleEn: String,
    val challengeTitleBn: String,
    val xpReward: Int,
    val takaReward: Int,
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
    val missionObjectiveBn: String,
    val missionXpReward: Int = 120,
    val missionTakaReward: Int = 450
) {
    val isEnvironmentReady: Boolean
        get() = buildStatus == DistrictBuildStatus.PLAYABLE_STREAMED

    fun name(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) nameBn else nameEn
    fun challengeTitle(lang: AppLanguage): String =
        if (lang == AppLanguage.BENGALI) challengeTitleBn else challengeTitleEn
    fun landmark(lang: AppLanguage): String =
        if (lang == AppLanguage.BENGALI) landmarkBn else landmarkEn
    fun description(lang: AppLanguage): String =
        if (lang == AppLanguage.BENGALI) descriptionBn else descriptionEn
    fun missionTitle(lang: AppLanguage): String =
        if (lang == AppLanguage.BENGALI) missionTitleBn else missionTitleEn
    fun missionObjective(lang: AppLanguage): String =
        if (lang == AppLanguage.BENGALI) missionObjectiveBn else missionObjectiveEn
}

object DistrictCatalog {
    val allDistricts: List<DistrictEntry> = buildList {
        var order = 1
        fun addDistrict(
            id: String,
            division: Division,
            nameEn: String,
            nameBn: String,
            challengeEn: String,
            challengeBn: String,
            xp: Int,
            taka: Int
        ) {
            val meta = DistrictWorldMetadata.metaFor(id, division, nameEn, nameBn)
            add(
                DistrictEntry(
                    id = id,
                    division = division,
                    unlockOrder = order++,
                    nameEn = nameEn,
                    nameBn = nameBn,
                    sceneKey = "scene_district_$id",
                    challengeId = "challenge_$id",
                    challengeTitleEn = challengeEn,
                    challengeTitleBn = challengeBn,
                    xpReward = xp,
                    takaReward = taka,
                    biome = meta.biome,
                    buildStatus = meta.buildStatus,
                    mapX = meta.mapX,
                    mapY = meta.mapY,
                    landmarkEn = meta.landmarkEn,
                    landmarkBn = meta.landmarkBn,
                    descriptionEn = meta.descriptionEn,
                    descriptionBn = meta.descriptionBn,
                    missionTitleEn = meta.missionTitleEn,
                    missionTitleBn = meta.missionTitleBn,
                    missionObjectiveEn = meta.missionObjectiveEn,
                    missionObjectiveBn = meta.missionObjectiveBn
                )
            )
        }

        // 1. Dhaka Division (13)
        addDistrict("dhaka", Division.DHAKA, "Dhaka", "ঢাকা", "Dhaka Plaza Defense", "ঢাকা প্লাজা প্রতিরক্ষা", 250, 1000)
        addDistrict("gazipur", Division.DHAKA, "Gazipur", "গাজীপুর", "Industrial Corridor Sweep", "শিল্প করিডোর অভিযান", 260, 1050)
        addDistrict("narayanganj", Division.DHAKA, "Narayanganj", "নারায়ণগঞ্জ", "Riverport Perimeter", "নদীবন্দর সুরক্ষা", 270, 1100)
        addDistrict("tangail", Division.DHAKA, "Tangail", "টাঙ্গাইল", "Madhupur Forest Recon", "মধুপুর বনাঞ্চল রিকন", 280, 1150)
        addDistrict("manikganj", Division.DHAKA, "Manikganj", "মানিকগঞ্জ", "Highway Checkpoint", "মহাসড়ক চেকপয়েন্ট", 290, 1200)
        addDistrict("munshiganj", Division.DHAKA, "Munshiganj", "মুন্সীগঞ্জ", "Padma Bridge Approach", "পদ্মা সেতু অ্যাপ্রোচ", 300, 1250)
        addDistrict("narsingdi", Division.DHAKA, "Narsingdi", "নরসিংদী", "Textile Route Escort", "টেক্সটাইল রুট এসকর্ট", 310, 1300)
        addDistrict("kishoreganj", Division.DHAKA, "Kishoreganj", "কিশোরগঞ্জ", "Haor Causeway Sprint", "হাওর কজওয়ে স্প্রিন্ট", 320, 1350)
        addDistrict("faridpur", Division.DHAKA, "Faridpur", "ফরিদপুর", "Riverbank Holdout", "নদী তীর প্রতিরক্ষা", 330, 1400)
        addDistrict("gopalganj", Division.DHAKA, "Gopalganj", "গোপালগঞ্জ", "Canal Convoy Defense", "খাল কনভয় প্রতিরক্ষা", 340, 1450)
        addDistrict("madaripur", Division.DHAKA, "Madaripur", "মাদারীপুর", "Ferry Terminal Clash", "ফেরি টার্মিনাল সংঘর্ষ", 350, 1500)
        addDistrict("rajbari", Division.DHAKA, "Rajbari", "রাজবাড়ী", "Rail Yard Pursuit", "রেল ইয়ার্ড ধাওয়া", 360, 1550)
        addDistrict("shariatpur", Division.DHAKA, "Shariatpur", "শরীয়তপুর", "Delta Crossing", "ডেল্টা ক্রসিং", 370, 1600)

        // 2. Chattogram Division (11)
        addDistrict("chattogram", Division.CHATTOGRAM, "Chattogram", "চট্টগ্রাম", "Port Container Raid", "বন্দর কন্টেইনার রেইড", 400, 1800)
        addDistrict("coxs_bazar", Division.CHATTOGRAM, "Cox's Bazar", "কক্সবাজার", "Marine Drive Pursuit", "মেরিন ড্রাইভ ধাওয়া", 410, 1850)
        addDistrict("cumilla", Division.CHATTOGRAM, "Cumilla", "কুমিল্লা", "Mainamati Ridge Ops", "ময়নামতি রিজ অপস", 420, 1900)
        addDistrict("feni", Division.CHATTOGRAM, "Feni", "ফেনী", "Trunk Road Ambush", "ট্রাঙ্ক রোড অ্যামবুশ", 430, 1950)
        addDistrict("noakhali", Division.CHATTOGRAM, "Noakhali", "নোয়াখালী", "Island Ferry Escape", "দ্বীপ ফেরি পলায়ন", 440, 2000)
        addDistrict("lakshmipur", Division.CHATTOGRAM, "Lakshmipur", "লক্ষ্মীপুর", "Coastal Patrol", "উপকূলীয় টহল", 450, 2050)
        addDistrict("chandpur", Division.CHATTOGRAM, "Chandpur", "চাঁদপুর", "Meghna Confluence", "মেঘনা মোহনা মিশন", 460, 2100)
        addDistrict("brahmanbaria", Division.CHATTOGRAM, "Brahmanbaria", "ব্রাহ্মণবাড়িয়া", "Titas Bridge Guard", "তিতাস সেতু প্রহরা", 470, 2150)
        addDistrict("rangamati", Division.CHATTOGRAM, "Rangamati", "রাঙ্গামাটি", "Kaptai Lake Recon", "কাপ্তাই লেক রিকন", 480, 2200)
        addDistrict("khagrachhari", Division.CHATTOGRAM, "Khagrachhari", "খাগড়াছড়ি", "Hill Crest Parkour", "পাহাড়ি চূড়া পার্কোর", 490, 2250)
        addDistrict("bandarban", Division.CHATTOGRAM, "Bandarban", "বান্দরবান", "Cloud Peak Summit", "মেঘলা চূড়া অভিযান", 500, 2300)

        // 3. Rajshahi Division (8)
        addDistrict("rajshahi", Division.RAJSHAHI, "Rajshahi", "রাজশাহী", "Padma Embankment Run", "পদ্মা বাঁধ অভিযান", 520, 2400)
        addDistrict("bogura", Division.RAJSHAHI, "Bogura", "বগুড়া", "Mahasthangarh Siege", "মহাস্থানগড় অবরোধ", 530, 2450)
        addDistrict("joypurhat", Division.RAJSHAHI, "Joypurhat", "জয়পুরহাট", "Northern Rail Watch", "উত্তর রেল প্রহরা", 540, 2500)
        addDistrict("naogaon", Division.RAJSHAHI, "Naogaon", "নওগাঁ", "Vihara Courtyard", "বিহার প্রাঙ্গণ মিশন", 550, 2550)
        addDistrict("natore", Division.RAJSHAHI, "Natore", "নাটোর", "Palace Courtyard Ops", "রাজবাড়ি প্রাঙ্গণ অপস", 560, 2600)
        addDistrict("chapainawabganj", Division.RAJSHAHI, "Chapainawabganj", "চাঁপাইনবাবগঞ্জ", "Orchard Outpost", "বাগান আউটপোস্ট", 570, 2650)
        addDistrict("pabna", Division.RAJSHAHI, "Pabna", "পাবনা", "Hardinge Bridge Watch", "হার্ডিঞ্জ ব্রিজ প্রহরা", 580, 2700)
        addDistrict("sirajganj", Division.RAJSHAHI, "Sirajganj", "সিরাজগঞ্জ", "Jamuna Span Defense", "যমুনা সেতু প্রতিরক্ষা", 590, 2750)

        // 4. Khulna Division (10)
        addDistrict("khulna", Division.KHULNA, "Khulna", "খুলনা", "Rupsha Shipyard", "রূপসা শিপইয়ার্ড", 610, 2800)
        addDistrict("bagerhat", Division.KHULNA, "Bagerhat", "বাগেরহাট", "Sundarbans Edge Patrol", "সুন্দরবন প্রান্ত টহল", 620, 2850)
        addDistrict("satkhira", Division.KHULNA, "Satkhira", "সাতক্ষীরা", "Mangrove Channel Ops", "ম্যানগ্রোভ চ্যানেল অপস", 630, 2900)
        addDistrict("jashore", Division.KHULNA, "Jashore", "যশোর", "Airfield Perimeter", "এয়ারফিল্ড পেরিমিটার", 640, 2950)
        addDistrict("jhenaidah", Division.KHULNA, "Jhenaidah", "ঝিনাইদহ", "Town Hall Holdout", "টাউন হল হোল্ডআউট", 650, 3000)
        addDistrict("magura", Division.KHULNA, "Magura", "মাগুরা", "Nabaganga Drift", "নবগঙ্গা ড্রিফট", 660, 3050)
        addDistrict("narail", Division.KHULNA, "Narail", "নড়াইল", "Chitra Boat Escort", "চিত্রা বোট এসকর্ট", 670, 3100)
        addDistrict("kushtia", Division.KHULNA, "Kushtia", "কুষ্টিয়া", "Lalon Bridge Crossing", "লালন সেতু ক্রসিং", 680, 3150)
        addDistrict("chuadanga", Division.KHULNA, "Chuadanga", "চুয়াডাঙ্গা", "West Junction Dash", "পশ্চিম জংশন ড্যাশ", 690, 3200)
        addDistrict("meherpur", Division.KHULNA, "Meherpur", "মেহেরপুর", "Heritage Plaza Guard", "হেরিটেজ প্লাজা প্রহরা", 700, 3250)

        // 5. Barishal Division (6)
        addDistrict("barishal", Division.BARISHAL, "Barishal", "বরিশাল", "Kirtonkhola River Run", "কীর্তনখোলা নদী অভিযান", 720, 3300)
        addDistrict("barguna", Division.BARISHAL, "Barguna", "বরগুনা", "Estuary Beacon Hold", "মোহনা বিকন হোল্ড", 730, 3350)
        addDistrict("bhola", Division.BARISHAL, "Bhola", "ভোলা", "Island Harbor Raid", "দ্বীপ হারবার রেইড", 740, 3400)
        addDistrict("jhalokati", Division.BARISHAL, "Jhalokathi", "ঝালকাঠি", "Floating Market Sprint", "ভাসমান বাজার স্প্রিন্ট", 750, 3450)
        addDistrict("patuakhali", Division.BARISHAL, "Patuakhali", "পটুয়াখালী", "Kuakata Coast Dash", "কুয়াকাটা উপকূল ড্যাশ", 760, 3500)
        addDistrict("pirojpur", Division.BARISHAL, "Pirojpur", "পিরোজপুর", "Baleshwar Canal Ops", "বলেশ্বর খাল অপস", 770, 3550)

        // 6. Sylhet Division (4)
        addDistrict("sylhet", Division.SYLHET, "Sylhet", "সিলেট", "Keane Bridge Defense", "ক্বীন ব্রিজ প্রতিরক্ষা", 800, 3700)
        addDistrict("moulvibazar", Division.SYLHET, "Moulvibazar", "মৌলভীবাজার", "Lawachhara Trail", "লাউয়াছড়া ট্রেইল", 810, 3750)
        addDistrict("habiganj", Division.SYLHET, "Habiganj", "হবিগঞ্জ", "Tea Estate Stealth", "চা বাগান স্টেলথ", 820, 3800)
        addDistrict("sunamganj", Division.SYLHET, "Sunamganj", "সুনামগঞ্জ", "Tanguar Haor Boat Ops", "টাঙ্গুয়ার হাওর বোট অপস", 830, 3850)

        // 7. Rangpur Division (8)
        addDistrict("rangpur", Division.RANGPUR, "Rangpur", "রংপুর", "Carmichael Plaza Ops", "কারমাইকেল প্লাজা অপস", 860, 4000)
        addDistrict("dinajpur", Division.RANGPUR, "Dinajpur", "দিনাজপুর", "Ramsagar Outpost", "রামসাগর আউটপোস্ট", 870, 4050)
        addDistrict("kurigram", Division.RANGPUR, "Kurigram", "কুড়িগ্রাম", "Dharla River Watch", "ধরলা নদী প্রহরা", 880, 4100)
        addDistrict("gaibandha", Division.RANGPUR, "Gaibandha", "গাইবান্ধা", "Teesta Char Patrol", "তিস্তা চর টহল", 890, 4150)
        addDistrict("nilphamari", Division.RANGPUR, "Nilphamari", "নীলফামারী", "Syedpur Rail Hub", "সৈয়দপুর রেল হাব", 900, 4200)
        addDistrict("panchagarh", Division.RANGPUR, "Panchagarh", "পঞ্চগড়", "Northern Frontier Sprint", "উত্তর সীমান্ত স্প্রিন্ট", 910, 4250)
        addDistrict("thakurgaon", Division.RANGPUR, "Thakurgaon", "ঠাকুরগাঁও", "Airfield Ruins Recon", "পুরাতন এয়ারফিল্ড রিকন", 920, 4300)
        addDistrict("lalmonirhat", Division.RANGPUR, "Lalmonirhat", "লালমনিরহাট", "Teesta Barrage Guard", "তিস্তা ব্যারেজ প্রহরা", 930, 4350)

        // 8. Mymensingh Division (4)
        addDistrict("mymensingh", Division.MYMENSINGH, "Mymensingh", "ময়মনসিংহ", "Brahmaputra Bank Ops", "ব্রহ্মপুত্র তীর অপস", 960, 4500)
        addDistrict("jamalpur", Division.MYMENSINGH, "Jamalpur", "জামালপুর", "Bahadurabad Ghat", "বাহাদুরাবাদ ঘাট", 970, 4550)
        addDistrict("netrokona", Division.MYMENSINGH, "Netrokona", "নেত্রকোনা", "Birishiri Ridge Run", "বিরিশিরি রিজ রান", 980, 4600)
        addDistrict("sherpur", Division.MYMENSINGH, "Sherpur", "শেরপুর", "Garo Foothills Boss", "গারো পাহাড় বস ফাইট", 1200, 5000)
    }

    val playableDistricts: List<DistrictEntry>
        get() = allDistricts.filter { it.isEnvironmentReady }

    fun getById(id: String): DistrictEntry =
        allDistricts.firstOrNull { it.id == id } ?: allDistricts.first()

    fun nextDistrictAfter(id: String, customOrder: List<String> = SequentialUnlockConfig.defaultOrder): DistrictEntry? =
        SequentialUnlockConfig.nextDistrictIdAfter(id, customOrder)?.let { getById(it) }
}
