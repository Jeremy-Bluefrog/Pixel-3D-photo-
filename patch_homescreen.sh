#!/bin/bash
awk '
/Button\(/ {
    skip_button = 1
}
/Icon\(imageVector = Icons.Default.AutoAwesome, contentDescription = null\)/ {
    if (skip_button) next
}
/Spacer\(modifier = Modifier.width\(8.dp\)\)/ {
    if (skip_button) next
}
/Text\(\"透過此相片學習並升級模型\"\)/ {
    if (skip_button) next
}
/\}/ {
    if (skip_button) {
        skip_button = 0
        print "                                                Row("
        print "                                                    modifier = Modifier.fillMaxWidth(),"
        print "                                                    horizontalArrangement = Arrangement.SpaceBetween,"
        print "                                                    verticalAlignment = Alignment.CenterVertically"
        print "                                                ) {"
        print "                                                    Column {"
        print "                                                        Text("
        print "                                                            text = \"允許透過相片學習並升級模型\", "
        print "                                                            style = MaterialTheme.typography.bodyMedium,"
        print "                                                            color = MaterialTheme.colorScheme.onSurface"
        print "                                                        )"
        print "                                                        Text("
        print "                                                            text = \"將學習結果回傳以改進核心神經網路\", "
        print "                                                            style = MaterialTheme.typography.bodySmall,"
        print "                                                            color = MaterialTheme.colorScheme.onSurfaceVariant"
        print "                                                        )"
        print "                                                    }"
        print "                                                    androidx.compose.material3.Switch("
        print "                                                        checked = true,"
        print "                                                        onCheckedChange = null,"
        print "                                                        enabled = false"
        print "                                                    )"
        print "                                                }"
        next
    }
}
{
    if (!skip_button) print $0
}
' app/src/main/java/com/example/ui/screens/HomeScreen.kt > app/src/main/java/com/example/ui/screens/HomeScreen.kt.new
mv app/src/main/java/com/example/ui/screens/HomeScreen.kt.new app/src/main/java/com/example/ui/screens/HomeScreen.kt
