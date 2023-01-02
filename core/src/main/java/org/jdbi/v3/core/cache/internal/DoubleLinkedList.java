/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.core.cache.internal;

final class DoubleLinkedList<K, V> {

    // rootNode double times as start (rootNode.right) and end (rootNode.left).
    // rootNode right <-> left <node> right <-> left <node> right <-> rootNode left

    final Node<K, V> rootNode;
    int size = 0;

    DoubleLinkedList() {
        this.rootNode = createNode(null, null);
        rootNode.right = rootNode;
        rootNode.left = rootNode;
    }

    void addHead(Node<K, V> node) {
        if (node == null) {
            return;
        }

        if (node.left != null || node.right != null) {
            throw new IllegalStateException("Can not add node twice!");
        }

        // slot in at the beginning
        node.left = rootNode;
        node.right = rootNode.right;
        node.left.right = node;
        node.right.left = node;

        size++;
    }

    Node<K, V> removeTail() {
        return removeNode(rootNode.left);
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    Node<K, V> removeNode(Node<K, V> node) {
        // check that the node is valid. The marker node can not be removed.
        // if any of the pointers is null, the node is not actually part of the
        // list.
        if (node == rootNode || node.left == null || node.right == null) {
            return null;
        }

        // unhook from the left side.
        node.left.right = node.right;

        // unhook the right side
        node.right.left = node.left;

        // clean out the node, don't want to leak pointers
        node.left = null;
        node.right = null;

        size--;
        return node;
    }

    static <K, V> Node<K, V> createNode(K key, V value) {
        return new Node<>(key, value);
    }

    @Override
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Node<K, V> node = rootNode.right;
        while (node != rootNode) {
            sb.append(node.key).append(" = ").append(node.value);

            if (node.right != null) {
                sb.append(", ");
            }
            node = node.right;
        }
        sb.append(']');
        return sb.toString();
    }

    static final class Node<K, V> {

        Node<K, V> left = null;
        Node<K, V> right = null;

        final K key;
        final V value;

        private Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
