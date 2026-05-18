/// <reference types="vite/client" />

// mantine styles
import "@mantine/core/styles.layer.css";
import "@mantine/dates/styles.css";
import "@mantine/notifications/styles.css";
import "@mantine/spotlight/styles.css";
import "@mantine/schedule/styles.css";
import "@mantine/carousel/styles.css";

// custom styles
import.meta.glob("./styles/**/*.css", { eager: true });

// atproto services
import "./lib/atproto/atproto-services";

// dayjs Locales
import "dayjs";
import.meta.glob("../node_modules/dayjs/esm/locale/*.js", { eager: true });

// polyfills
import "temporal-polyfill-lite/global";

// database
import "./db/drizzle";

// events
import "./init-events";
