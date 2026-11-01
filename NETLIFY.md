# Deploy to Netlify

The React frontend builds into the single `out` folder at the project root.
Netlify serves this folder. The Spring Boot Java backend must run separately
on a host that supports Java 21, with its database, Redis and service credentials.

## Configure the frontend

Set these build environment variables in Netlify, or in
`listifyai-frontend/.env.production.local` before building locally:

```dotenv
VITE_API_URL=https://your-backend.example.com
VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_ANON_KEY=your-public-supabase-anon-key
VITE_ENABLE_DEV_LOGIN=false
```

Use only the public Supabase anon key here. Backend secrets belong on the Java host.
Vite embeds these settings during the build, so rebuild after changing them.
The existing local API URL points to localhost; until a hosted HTTPS API URL is
provided, the exported site can display its pages but backend features cannot work.

## Build and upload

From the project root:

```powershell
npm.cmd --prefix listifyai-frontend run lint
npm.cmd --prefix listifyai-frontend run build
```

Upload the `out` folder to Netlify's manual deployment page. It includes the SPA
rewrite file so refreshing routes such as `/login` works.

For Git-based deployment, the root `netlify.toml` configures dependency installation,
the build command, Node 22 and the `out` publish directory automatically.

## Connect authentication and the backend

On the Java host, set `FRONTEND_URL` and `CORS_ALLOWED_ORIGINS` to your actual
Netlify HTTPS origin (no trailing slash). Keep developer authentication disabled
and do not use the `dev` Spring profile in production.

In Supabase Authentication URL Configuration, set the Site URL to your Netlify
origin and allow your site's `/auth/callback` URL. Verify login and an authenticated
API request after deployment.

Reference: https://docs.netlify.com/build/frameworks/framework-setup-guides/vite/
