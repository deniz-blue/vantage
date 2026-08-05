Array.prototype.toSorted ??= function (comparator?: (a: any, b: any) => number) {
	return [...this].sort(comparator);
};

Array.prototype.toReversed ??= function () {
	return [...this].reverse();
};

Array.prototype.findLast ??= function (
	predicate: (value: any, index: number, array: any[]) => boolean,
) {
	for (let i = this.length - 1; i >= 0; i--) {
		if (predicate(this[i], i, this)) return this[i];
	}
};

Array.prototype.findLastIndex ??= function (
	predicate: (value: any, index: number, array: any[]) => boolean,
) {
	for (let i = this.length - 1; i >= 0; i--) {
		if (predicate(this[i], i, this)) return i;
	}
	return -1;
};
