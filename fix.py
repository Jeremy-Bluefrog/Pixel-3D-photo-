import re

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    content = f.read()

# the syntax is messed up. I will replace the messed up parts.
# The original code before patch_homescreen.sh was fine.
