package me.zero.rdi.util;

import org.objectweb.asm.Opcodes;

import java.util.HashSet;
import java.util.Set;

public class Constants {
    public static final Set<String> COLLECTIONS = new HashSet<String>() {
        private static final long serialVersionUID = -3779578266088390366L;

        {
            add("Ljava/util/Vector;");
            add("Ljava/util/List;");
            add("Ljava/util/ArrayList;");
            add("Ljava/util/Collection;");
            add("Ljava/util/AbstractCollection;");
            add("Ljava/util/AbstractList;");
            add("Ljava/util/AbstractSet;");
            add("Ljava/util/AbstractQueue;");
            add("Ljava/util/HashSet;");
            add("Ljava/util/Set;");
            add("Ljava/util/Queue;");
            add("Ljava/util/concurrent/ArrayBlockingQueue;");
            add("Ljava/util/concurrent/ConcurrentLinkedQueue;");
            add("Ljava/util/concurrent/ConcurrentLinkedQueue;");
            add("Ljava/util/concurrent/DelayQueue;");
            add("Ljava/util/concurrent/LinkedBlockingQueue;");
            add("Ljava/util/concurrent/SynchronousQueue;");
            add("Ljava/util/concurrent/BlockingQueue;");
            add("Ljava/util/concurrent/BlockingDeque;");
            add("Ljava/util/concurrent/LinkedBlockingDeque;");
            add("Ljava/util/concurrent/ConcurrentLinkedDeque;");
            add("Ljava/util/Deque;");
            add("Ljava/util/ArrayDeque;");
        }
    };

    public static final Set<String> ITERABLES = new HashSet<String>() {
        private static final long serialVersionUID = -3779578266088390365L;

        {
            add("Ljava/util/Vector;");
            add("Ljava/util/List;");
            add("Ljava/util/ArrayList;");
            add("Ljava/util/Collection;");
            add("Ljava/util/AbstractCollection;");
            add("Ljava/util/AbstractList;");
            add("Ljava/util/AbstractSet;");
            add("Ljava/util/AbstractQueue;");
            add("Ljava/util/HashSet;");
            add("Ljava/util/Set;");
            add("Ljava/util/Queue;");
            add("Ljava/util/concurrent/ArrayBlockingQueue;");
            add("Ljava/util/concurrent/ConcurrentLinkedQueue;");
            add("Ljava/util/concurrent/ConcurrentLinkedQueue;");
            add("Ljava/util/concurrent/DelayQueue;");
            add("Ljava/util/concurrent/LinkedBlockingQueue;");
            add("Ljava/util/concurrent/SynchronousQueue;");
            add("Ljava/util/concurrent/BlockingQueue;");
            add("Ljava/util/concurrent/BlockingDeque;");
            add("Ljava/util/concurrent/LinkedBlockingDeque;");
            add("Ljava/util/concurrent/ConcurrentLinkedDeque;");
            add("Ljava/util/Deque;");
            add("Ljava/util/ArrayDeque;");
            add("Ljava/lang/Iterable;");
        }
    };

    public static final int VISIBILITY_MODIFIERS = Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_PUBLIC;
}
