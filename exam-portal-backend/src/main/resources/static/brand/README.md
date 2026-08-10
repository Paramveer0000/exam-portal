# Report branding

`mentalist-logo.png` is the logo the PDF report falls back to when no platform
logo has been uploaded.

Resolution order (see `ReportBrandingServiceImpl`):

1. `platform_settings.company_logo` - what a SUPER_ADMIN uploaded via
   `PUT /api/platform/branding`. A deployment can rebrand without a rebuild.
2. `classpath:brand/mentalist-logo.png` - this file.
3. Neither present: the report renders a text wordmark instead of a broken image.

Drop the company logo here as **`mentalist-logo.png`**. A square PNG around
600x600 is plenty - it is embedded into every generated PDF as a base64 data
URL, so keep the file small.
