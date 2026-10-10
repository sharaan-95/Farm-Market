package com.farmmarket

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------- Login
@Composable
fun LoginScreen(vm: AppVm) {
    var phone by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("FarmMarket", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(vm.t("From Farm to Market"))
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(10) },
            label = { Text(vm.t("Phone number")) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(pw, { pw = it }, label = { Text(vm.t("Password")) }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(16.dp))
        Button({ vm.login(phone, pw) }, Modifier.fillMaxWidth(),
            enabled = !vm.loading && phone.length == 10 && pw.isNotEmpty()) {
            if (vm.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(vm.t("Login"))
        }
        TextButton({ vm.demoLogin() }, Modifier.fillMaxWidth()) { Text(vm.t("Continue in Demo Mode")) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton({ vm.setLang("en") }) { Text("English") }
            TextButton({ vm.setLang("ta") }) { Text("தமிழ்") }
        }
    }
}

// ---------------------------------------------------------------- Home shell
@Composable
fun HomeScreen(vm: AppVm) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(bottomBar = {
        NavigationBar {
            listOf("Market" to Icons.Filled.Home, "Sell" to Icons.Filled.Add, "Profile" to Icons.Filled.Person)
                .forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(tab == i, { tab = i }, { Icon(icon, null) }, label = { Text(vm.t(label)) })
                }
        }
    }) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) { 0 -> MarketScreen(vm); 1 -> SellScreen(vm); else -> ProfileScreen(vm) }
        }
    }
}

// ---------------------------------------------------------------- Market
@Composable
fun MarketScreen(vm: AppVm) {
    val latest = vm.prices.groupBy { it.cropId to it.marketName }.values.map { g -> g.maxBy { it.priceDate } }
    val byCrop = latest.groupBy { it.cropName }.toSortedMap()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { StatusRow(vm) }
        if (byCrop.isEmpty()) item { Text(vm.t("No price data yet. Connect to the server once.")) }
        byCrop.forEach { (crop, rows) ->
            item(key = crop) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(crop, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        rows.sortedByDescending { it.price }.forEach { r ->
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(r.marketName)
                                    Text("${r.marketType} • ${r.distanceKm.toInt()} km", style = MaterialTheme.typography.bodySmall)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("₹${r.price.toInt()}/${r.unit}", fontWeight = FontWeight.Bold)
                                    when (r.trend) {
                                        "UP" -> Text("▲", color = Color(0xFF2E7D32))
                                        "DOWN" -> Text("▼", color = Color(0xFFC62828))
                                        else -> Text("–")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(vm: AppVm) {
    val time = if (vm.lastUpdated > 0)
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(vm.lastUpdated)) else "-"
    val (text, color) = when {
        vm.demo -> vm.t("Demo Data") to Color(0xFF6A1B9A)
        vm.offline -> "${vm.t("Offline mode")} • ${vm.t("Last updated")}: $time" to Color(0xFFE65100)
        vm.pending > 0 -> "${vm.pending} ${vm.t("changes waiting to sync")}" to Color(0xFFF9A825)
        else -> vm.t("All data synced") to Color(0xFF2E7D32)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = color, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        TextButton({ vm.refresh() }) { Text(vm.t("Refresh")) }
    }
}

// ---------------------------------------------------------------- Sell
@Composable
fun SellScreen(vm: AppVm) {
    val crops = vm.prices.map { it.cropId to it.cropName }.distinct().sortedBy { it.second }
    var crop by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var open by remember { mutableStateOf(false) }
    var qty by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("kg") }
    var price by remember { mutableStateOf("") }
    var variety by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    val num = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Box {
                OutlinedButton({ open = true }, Modifier.fillMaxWidth()) { Text(crop?.second ?: vm.t("Select crop")) }
                DropdownMenu(open, { open = false }) {
                    crops.forEach { c -> DropdownMenuItem({ Text(c.second) }, { crop = c; open = false }) }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(qty, { qty = it }, label = { Text(vm.t("Quantity")) }, singleLine = true,
                    keyboardOptions = num, modifier = Modifier.weight(1f))
                OutlinedTextField(unit, { unit = it }, label = { Text(vm.t("Unit")) }, singleLine = true, modifier = Modifier.weight(0.6f))
            }
        }
        item { OutlinedTextField(price, { price = it }, label = { Text(vm.t("Expected price")) }, singleLine = true, keyboardOptions = num, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(variety, { variety = it }, label = { Text(vm.t("Variety")) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(location, { location = it }, label = { Text(vm.t("Location")) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(desc, { desc = it }, label = { Text(vm.t("Description")) }, modifier = Modifier.fillMaxWidth()) }
        item {
            Button({
                val c = crop; val q = qty.toDoubleOrNull()
                if (c != null && q != null && q > 0) {
                    vm.createListing(c.first, c.second, q, unit.ifBlank { "kg" }, price.toDoubleOrNull(), variety, location, desc)
                    msg = if (vm.offline) vm.t("Saved offline — waiting for sync.") else vm.t("Listing saved.")
                    qty = ""; price = ""; variety = ""; desc = ""
                }
            }, Modifier.fillMaxWidth(), enabled = crop != null && (qty.toDoubleOrNull() ?: 0.0) > 0) { Text(vm.t("Save listing")) }
            msg?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp)) }
        }
        if (vm.listings.isNotEmpty()) item { Text(vm.t("My listings"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
        items(vm.listings, key = { it.uuid }) { l ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${l.crop} — ${l.qty} ${l.unit}")
                    Text(when (l.status) {
                        "PUBLISHED" -> vm.t("Published"); "FAILED" -> vm.t("Failed")
                        "LOCAL" -> vm.t("Saved (demo)"); else -> vm.t("Waiting to sync")
                    }, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Profile
@Composable
fun ProfileScreen(vm: AppVm) {
    var url by remember { mutableStateOf(vm.serverUrl) }
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(vm.userName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(vm.t("Language"), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(vm.lang == "en", { vm.setLang("en") }, { Text("English") })
            FilterChip(vm.lang == "ta", { vm.setLang("ta") }, { Text("தமிழ்") })
        }
        Text(vm.t("Server address"), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(url, { url = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button({ vm.setServer(url) }) { Text(vm.t("Save")) }
        OutlinedButton({ vm.logout() }, Modifier.fillMaxWidth()) { Text(vm.t("Logout")) }
    }
}
