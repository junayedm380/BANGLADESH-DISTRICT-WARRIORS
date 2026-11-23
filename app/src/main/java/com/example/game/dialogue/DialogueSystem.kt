package com.example.game.dialogue

import com.example.game.localization.AppLanguage

data class DialogueLine(
    val id: String,
    val speakerEn: String,
    val speakerBn: String,
    val textEn: String,
    val textBn: String
) {
    fun speaker(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) speakerBn else speakerEn
    fun text(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) textBn else textEn
}

object DialogueCatalog {
    val trainingOfficerLines = listOf(
        DialogueLine(
            id = "dhaka_welcome",
            speakerEn = "Commander Karim • Training Officer",
            speakerBn = "কমান্ডার করিম • প্রশিক্ষণ কর্মকর্তা",
            textEn = "Welcome to the Dhaka Training Ground. Begin your mission.",
            textBn = "ঢাকার ট্রেনিং গ্রাউন্ডে স্বাগতম। তোমার অভিযান শুরু করো।"
        ),
        DialogueLine(
            id = "dhaka_controls",
            speakerEn = "Commander Karim • Training Officer",
            speakerBn = "কমান্ডার করিম • প্রশিক্ষণ কর্মকর্তা",
            textEn = "Test your aim on the range targets, switch between 1P and 3P cameras, and step into the emerald plaza beacon when ready for the District Challenge.",
            textBn = "রেঞ্জ টার্গেটে নিশানা পরীক্ষা করো, ১পি ও ৩পি ক্যামেরা বদলে দেখো এবং জেলা চ্যালেঞ্জের জন্য প্রস্তুত হলে সবুজ প্লাজা বিকনে যাও।"
        )
    )

    val vehicleZoneInfo = DialogueLine(
        id = "vehicle_pad_info",
        speakerEn = "Dhaka Vehicle Depot • Prototype Notice",
        speakerBn = "ঢাকা ভেহিকল ডিপো • প্রোটোটাইপ তথ্য",
        textEn = "Prototype Dhaka Jeep & Rickshaw models are staged here for visual testing. Drivable cars, motorcycles, buses, trucks, and boats unlock in Milestone 3.",
        textBn = "দৃশ্যমান পরীক্ষার জন্য প্রোটোটাইপ ঢাকা জিপ ও রিকশা মডেল এখানে রাখা হয়েছে। চালানো যায় এমন গাড়ি, মোটরসাইকেল, বাস, ট্রাক ও নৌকা মাইলস্টোন ৩-এ যুক্ত হবে।"
    )
}
