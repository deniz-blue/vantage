import { db, schema } from "@vantage/db";
import { eq } from "drizzle-orm";

export const TagsManager = new class {
	async getAll(): Promise<schema.Tag[]> {
		return await db
			.select()
			.from(schema.tags);
	}

	async getById(id: schema.Tag["id"]): Promise<schema.Tag | null> {
		return await db
			.select()
			.from(schema.tags)
			.where(eq(schema.tags.id, id))
			.then(rows => rows[0] ?? null);
	}

	async update(id: schema.Tag["id"], values: Pick<schema.Tag, "name" | "color">): Promise<void> {
		await db
			.update(schema.tags)
			.set({
				name: values.name,
				color: values.color,
				updatedAt: Temporal.Now.instant(),
			})
			.where(eq(schema.tags.id, id));
	}

	async create(values: Pick<schema.Tag, "name" | "color">): Promise<schema.Tag> {
		const [tag] = await db
			.insert(schema.tags)
			.values({
				id: crypto.randomUUID(),
				name: values.name,
				color: values.color,
				updatedAt: Temporal.Now.instant(),
			})
			.returning();

		return tag!;
	}

	async delete(id: schema.Tag["id"]): Promise<void> {
		await db
			.delete(schema.tags)
			.where(eq(schema.tags.id, id));
	}
};
