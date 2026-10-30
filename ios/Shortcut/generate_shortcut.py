import plistlib
import sys

GET_WALLPAPER_UUID = "6F1E5C0A-2B7D-4C8E-9A31-5D2F7B8C9E10"

WORKFLOW = {
    "WFWorkflowClientVersion": "2607.0.2",
    "WFWorkflowMinimumClientVersion": 900,
    "WFWorkflowMinimumClientVersionString": "900",
    "WFWorkflowHasOutputFallback": False,
    "WFWorkflowHasShortcutInputVariables": False,
    "WFWorkflowIcon": {
        "WFWorkflowIconStartColor": 431817727,
        "WFWorkflowIconGlyphNumber": 59446,
    },
    "WFWorkflowImportQuestions": [],
    "WFWorkflowInputContentItemClasses": [],
    "WFWorkflowOutputContentItemClasses": [],
    "WFQuickActionSurfaces": [],
    "WFWorkflowTypes": [],
    "WFWorkflowActions": [
        {
            "WFWorkflowActionIdentifier": "dev.marujo.gitwallpaper.GetWallpaperIntent",
            "WFWorkflowActionParameters": {
                "AppIntentDescriptor": {
                    "AppIntentIdentifier": "GetWallpaperIntent",
                    "BundleIdentifier": "dev.marujo.gitwallpaper",
                    "Name": "Git Wallpaper",
                    "TeamIdentifier": "8VRVBA2X7C",
                    "ActionRequiresAppInstallation": True,
                },
                "UUID": GET_WALLPAPER_UUID,
            },
        },
        {
            "WFWorkflowActionIdentifier": "is.workflow.actions.wallpaper.set",
            "WFWorkflowActionParameters": {
                "WFWallpaperShowPreview": False,
                "WFWallpaperSmartCrop": False,
                "WFWallpaperLegibilityBlur": False,
            },
        },
    ],
}


def main():
    with open(sys.argv[1], "wb") as output:
        plistlib.dump(WORKFLOW, output, fmt=plistlib.FMT_BINARY)


if __name__ == "__main__":
    main()
