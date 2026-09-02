/**
 * LRU（Least Recently Used）缓存
 * - get / put 时间复杂度 O(1)
 * - 使用 Map + 双向链表实现
 */

class LruNode<K, V> {
    key: K;
    value: V;

    prev: LruNode<K, V> | null = null;
    next: LruNode<K, V> | null = null;

    constructor(key: K, value: V) {
        this.key = key;
        this.value = value;
    }
}

class LruList<K, V> {
    tag: LruNode<K, V>;
    size: number = 0;

    constructor() {
        this.tag = new LruNode(null as any, null as any);
        this.tag.prev = this.tag;
        this.tag.next = this.tag;
    }

    addFirst(o: LruNode<K, V>) {
        o.prev = this.tag;
        o.next = this.tag.next;
        o.next!.prev = o;
        o.prev!.next = o;
        this.size++;
    }

    remove(o: LruNode<K, V>): LruNode<K, V> | null {
        o.prev!.next = o.next;
        o.next!.prev = o.prev;
        this.size--;
        return o;
    }

    popLast(): LruNode<K, V> | null {
        let last = this.tag.prev;
        if (last == this.tag) return null;
        return this.remove(last!);
    }

    toArray(): V[] {
        let arr: V[] = new Array(this.size);
        let o = this.tag.next;
        for (let i = 0; i < arr.length; i++) {
            arr[i] = o!.value;
            o = o!.next;
        }
        return arr;
    }
}

export class LRUCache<K, V> {
    /**
     * 最大容量
     */
    private readonly capacity: number;

    /**
     * key -> node
     */
    private readonly map = new Map<K, LruNode<K, V>>();

    /**
     * 链表
     */
    private list: LruList<K, V> = new LruList<K, V>();

    constructor(capacity: number) {
        if (capacity <= 0) {
            throw new Error("capacity must be greater than 0");
        }
        this.capacity = capacity;
    }

    public get(key: K): V | null {
        let o = this.map.get(key);
        if (o == null) return null;
        this.list.remove(o);
        this.list.addFirst(o);
        return o.value;
    }

    public put(key: K, value: V): void {
        let old = this.map.get(key);
        if (old != null) {
            this.get(key);
            old.value = value;
            return;
        }
        if (this.map.size >= this.capacity) {
            let last = this.list.popLast();
            if (last != null) {
                this.map.delete(last.key);
            }
        }
        let node = new LruNode(key, value);
        this.list.addFirst(node);
        this.map.set(key, node);
    }
    public values(): V[] {
        return this.list.toArray()
    }
}
