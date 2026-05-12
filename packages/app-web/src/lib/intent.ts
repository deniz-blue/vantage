import { isCanonicalResourceUri } from "@atcute/lexicons";
import z from "zod";

export const AtUriSchema = z.string().refine(s => isCanonicalResourceUri(s), { message: "Invalid at URI" });
export const HttpUrlSchema = z.url({ protocol: /^(https?)$/ });

export const RemoteUriSchema = z.union([AtUriSchema, HttpUrlSchema]);
export type RemoteUri = z.infer<typeof RemoteUriSchema>;
