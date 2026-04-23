package gg.tropic.practice.extensions

fun formatPlayerPing(ping: Int) = when (true)
{
    (ping > 110) -> "&c"
    (ping > 60) -> "&e"
    else -> "&a"
}
