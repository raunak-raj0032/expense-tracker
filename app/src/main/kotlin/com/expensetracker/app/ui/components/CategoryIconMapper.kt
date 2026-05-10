package com.expensetracker.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.LocalAirport
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.LocalConvenienceStore
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalPostOffice
import androidx.compose.material.icons.filled.LocalPrintshop
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Support
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Category
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconMapper {
    private val iconMap = mapOf(
        "category" to Icons.Outlined.Category,
        "food" to Icons.Default.Restaurant,
        "dining" to Icons.Default.Restaurant,
        "groceries" to Icons.Default.LocalGroceryStore,
        "grocery" to Icons.Default.LocalGroceryStore,
        "transport" to Icons.Default.DirectionsCar,
        "transportation" to Icons.Default.DirectionsCar,
        "car" to Icons.Default.DirectionsCar,
        "gas" to Icons.Default.LocalGasStation,
        "fuel" to Icons.Default.LocalGasStation,
        "petrol" to Icons.Default.LocalGasStation,
        "bus" to Icons.Default.DirectionsBus,
        "metro" to Icons.Default.DirectionsSubway,
        "train" to Icons.Default.Train,
        "taxi" to Icons.Default.LocalTaxi,
        "uber" to Icons.Default.LocalTaxi,
        "ola" to Icons.Default.LocalTaxi,
        "auto" to Icons.Default.DirectionsCar,
        "shopping" to Icons.Default.ShoppingCart,
        "shopping_bag" to Icons.Default.ShoppingBag,
        "clothes" to Icons.Default.Checkroom,
        "clothing" to Icons.Default.Checkroom,
        "fashion" to Icons.Default.Checkroom,
        "electronics" to Icons.Default.Hardware,
        "gadgets" to Icons.Default.Smartphone,
        "phone" to Icons.Default.Phone,
        "mobile" to Icons.Default.Smartphone,
        "recharge" to Icons.Default.Smartphone,
        "recharges" to Icons.Default.Smartphone,
        "utilities" to Icons.Default.ElectricalServices,
        "electricity" to Icons.Default.Power,
        "electric" to Icons.Default.Power,
        "water" to Icons.Default.Water,
        "bill" to Icons.Default.Receipt,
        "bills" to Icons.Default.Receipt,
        "rent" to Icons.Default.Home,
        "house" to Icons.Default.Home,
        "home" to Icons.Default.Home,
        "insurance" to Icons.Default.HealthAndSafety,
        "medical" to Icons.Default.LocalHospital,
        "health" to Icons.Default.HealthAndSafety,
        "pharmacy" to Icons.Default.LocalPharmacy,
        "medicine" to Icons.Default.LocalPharmacy,
        "hospital" to Icons.Default.LocalHospital,
        "doctor" to Icons.Default.LocalHospital,
        "education" to Icons.Default.School,
        "school" to Icons.Default.School,
        "courses" to Icons.Default.School,
        "books" to Icons.Default.School,
        "entertainment" to Icons.Default.LocalMovies,
        "movies" to Icons.Default.Movie,
        "netflix" to Icons.Default.Subscriptions,
        "spotify" to Icons.Default.MusicNote,
        "subscription" to Icons.Default.Subscriptions,
        "subscriptions" to Icons.Default.Subscriptions,
        "ott" to Icons.Default.Subscriptions,
        "gaming" to Icons.Default.SportsEsports,
        "games" to Icons.Default.SportsEsports,
        "coffee" to Icons.Default.LocalCafe,
        "cafe" to Icons.Default.LocalCafe,
        "tea" to Icons.Default.LocalCafe,
        "restaurant" to Icons.Default.Restaurant,
        "fastfood" to Icons.Default.Fastfood,
        "pizza" to Icons.Default.Fastfood,
        "burger" to Icons.Default.Fastfood,
        "delivery" to Icons.Default.LocalShipping,
        "food_delivery" to Icons.Default.LocalShipping,
        "salary" to Icons.Default.AccountBalance,
        "income" to Icons.Default.AccountBalance,
        "investment" to Icons.Default.ShowChart,
        "investments" to Icons.Default.ShowChart,
        "mutual_fund" to Icons.Default.ShowChart,
        "stocks" to Icons.Default.ShowChart,
        "crypto" to Icons.Default.CurrencyBitcoin,
        "savings" to Icons.Default.Savings,
        "transfer" to Icons.Default.Sync,
        "gift" to Icons.Default.CardGiftcard,
        "gifts" to Icons.Default.CardGiftcard,
        "donation" to Icons.Default.VolunteerActivism,
        "donations" to Icons.Default.VolunteerActivism,
        "charity" to Icons.Default.VolunteerActivism,
        "travel" to Icons.Default.Flight,
        "flight" to Icons.Default.FlightTakeoff,
        "hotel" to Icons.Default.Hotel,
        "vacation" to Icons.Default.BeachAccess,
        "holiday" to Icons.Default.BeachAccess,
        "trip" to Icons.Default.Flight,
        "gym" to Icons.Default.FitnessCenter,
        "fitness" to Icons.Default.FitnessCenter,
        "exercise" to Icons.Default.DirectionsRun,
        "salon" to Icons.Default.Brush,
        "beauty" to Icons.Default.Brush,
        "grooming" to Icons.Default.Brush,
        "pet" to Icons.Default.Pets,
        "pets" to Icons.Default.Pets,
        "atm" to Icons.Default.LocalAtm,
        "bank" to Icons.Default.AccountBalance,
        "wifi" to Icons.Default.Wifi,
        "internet" to Icons.Default.Wifi,
        "data" to Icons.Default.Cloud,
        "mobile_data" to Icons.Default.Cloud,
        "communication" to Icons.Default.Smartphone,
        "misc" to Icons.Default.MoreVert,
        "other" to Icons.Default.MoreVert,
        "others" to Icons.Default.MoreVert,
        "miscellaneous" to Icons.Default.MoreVert,
        "office" to Icons.Default.Business,
        "work" to Icons.Default.Work,
        "software" to Icons.Default.Code,
        "tech" to Icons.Default.Computer,
        "tools" to Icons.Default.Build,
        "hardware" to Icons.Default.Hardware,
        "repair" to Icons.Default.HomeRepairService,
        "maintenance" to Icons.Default.Build,
        "wedding" to Icons.Default.Celebration,
        "party" to Icons.Default.Celebration,
        "celebration" to Icons.Default.Celebration,
        "events" to Icons.Default.Celebration,
        "birthday" to Icons.Default.Cake,
        "cake" to Icons.Default.Cake,
        "baby" to Icons.Default.ChildCare,
        "child" to Icons.Default.ChildCare,
        "kids" to Icons.Default.ChildCare,
        "tax" to Icons.Default.Calculate,
        "taxes" to Icons.Default.Calculate,
        "government" to Icons.Default.AccountBalance,
        "fine" to Icons.Default.Warning,
        "penalty" to Icons.Default.Warning,
        "fee" to Icons.Default.Receipt,
        "fees" to Icons.Default.Receipt,
        "membership" to Icons.Default.Subscriptions,
        "club" to Icons.Default.Subscriptions,
        "support" to Icons.Default.Support,
        "help" to Icons.Default.Support,
        "service" to Icons.Default.Support,
        "camera" to Icons.Default.PhotoCamera,
        "photo" to Icons.Default.CameraAlt,
        "photography" to Icons.Default.CameraAlt,
        "appliances" to Icons.Default.Kitchen,
        "kitchen" to Icons.Default.Kitchen,
        "furniture" to Icons.Default.Chair,
        "decor" to Icons.Default.Chair,
        "home_decor" to Icons.Default.Chair,
        "convenience" to Icons.Default.LocalConvenienceStore,
        "store" to Icons.Default.Store,
        "supermarket" to Icons.Default.LocalMall,
        "vegetables" to Icons.Default.Eco,
        "fruits" to Icons.Default.Eco,
        "organic" to Icons.Default.Eco,
        "snacks" to Icons.Default.Cookie,
        "chocolate" to Icons.Default.Cake,
        "icecream" to Icons.Default.Icecream,
        "bakery" to Icons.Default.BakeryDining,
        "bread" to Icons.Default.BakeryDining,
        "alcohol" to Icons.Default.LocalBar,
        "wine" to Icons.Default.LocalBar,
        "beer" to Icons.Default.LocalBar,
        "laundry" to Icons.Default.LocalLaundryService,
        "dry_cleaning" to Icons.Default.LocalLaundryService,
        "parking" to Icons.Default.LocalParking,
        "toll" to Icons.Default.LocalAtm,
        "flowers" to Icons.Default.LocalFlorist,
        "florist" to Icons.Default.LocalFlorist,
        "library" to Icons.Default.LocalLibrary,
        "post" to Icons.Default.LocalPostOffice,
        "printing" to Icons.Default.LocalPrintshop,
        "print" to Icons.Default.LocalPrintshop,
        "car_wash" to Icons.Default.LocalCarWash,
        "sports" to Icons.Default.Sports,
        "football" to Icons.Default.SportsSoccer,
        "cricket" to Icons.Default.Sports,
        "tennis" to Icons.Default.Sports,
        "swimming" to Icons.Default.Pool,
        "pool" to Icons.Default.Pool,
        "cycling" to Icons.Default.DirectionsBike,
        "bike" to Icons.Default.DirectionsBike,
        "lab" to Icons.Default.Science,
        "tests" to Icons.Default.Science,
        "video" to Icons.Default.Videocam,
        "youtube" to Icons.Default.Subscriptions,
        "amazon" to Icons.Default.ShoppingCart,
        "flipkart" to Icons.Default.ShoppingCart,
        "myntra" to Icons.Default.Checkroom,
        "ajio" to Icons.Default.Checkroom,
        "meesho" to Icons.Default.ShoppingCart,
        "credit_card" to Icons.Default.CreditCard,
        "emi" to Icons.Default.Payment,
        "loan" to Icons.Default.AccountBalance,
        "rented_equipment" to Icons.Default.Verified,
        "property_tax" to Icons.Default.Calculate,
        "society" to Icons.Default.Business,
        "paan" to Icons.Default.Store,
        "pan" to Icons.Default.Store,
        "zomato" to Icons.Default.Restaurant,
        "swiggy" to Icons.Default.Restaurant,
        "money" to Icons.Default.Money,
        "cash" to Icons.Default.Money
    )

    fun getIcon(iconKey: String): ImageVector {
        return iconMap[iconKey.lowercase()] ?: Icons.Outlined.Category
    }
}
