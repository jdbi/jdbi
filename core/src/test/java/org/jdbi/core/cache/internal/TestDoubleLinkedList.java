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
package org.jdbi.core.cache.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestDoubleLinkedList {

    @Test
    public void testEmptyList() {
        DoubleLinkedList<String, String> list = new DoubleLinkedList<>();

        assertThat(list.size).isZero();
        assertThat(list.rootNode.right).isSameAs(list.rootNode);
        assertThat(list.rootNode.left).isSameAs(list.rootNode);
    }

    @Test
    public void testRemoveEmptyTail() {
        DoubleLinkedList<String, String> list = new DoubleLinkedList<>();

        DoubleLinkedList.Node<String, String> node = list.removeTail();

        assertThat(node).isNull();

        assertThat(list.size).isZero();
        assertThat(list.rootNode.right).isSameAs(list.rootNode);
        assertThat(list.rootNode.left).isSameAs(list.rootNode);
    }

    @Test
    public void testRemoveRemovedNode() {
        DoubleLinkedList<String, String> list = new DoubleLinkedList<>();

        // add node
        DoubleLinkedList.Node<String, String> node = DoubleLinkedList.createNode("foo", "bar");
        list.addHead(node);
        assertThat(node.left).isNotNull();
        assertThat(node.right).isNotNull();

        // remove node from list, ensure it is really removed
        DoubleLinkedList.Node<String, String> removedNode = list.removeTail();
        assertThat(removedNode).isSameAs(node);
        assertThat(removedNode.left).isNull();
        assertThat(removedNode.right).isNull();

        // double remove is ok (but null)
        removedNode = list.removeNode(removedNode);
        assertThat(removedNode).isNull();
    }

    @Test
    public void testRemoveRootNode() {
        DoubleLinkedList<String, String> list = new DoubleLinkedList<>();

        DoubleLinkedList.Node<String, String> node = DoubleLinkedList.createNode("foo", "bar");
        list.addHead(node);

        assertThat(list.size).isOne();
        assertThat(list.rootNode.right).isSameAs(node);
        assertThat(list.rootNode.left).isSameAs(node);

        assertThat(node.left).isSameAs(list.rootNode);
        assertThat(node.right).isSameAs(list.rootNode);

        // can't remove the root node.
        DoubleLinkedList.Node<String, String> removedNode = list.removeNode(node.left);
        assertThat(removedNode).isNull();

        assertThat(list.size).isOne();
        assertThat(list.rootNode.right).isSameAs(node);
        assertThat(list.rootNode.left).isSameAs(node);
    }

    @Test
    public void testAddNode() {
        DoubleLinkedList<String, String> list = new DoubleLinkedList<>();

        DoubleLinkedList.Node<String, String> node = DoubleLinkedList.createNode("foo", "bar");

        list.addHead(node);

        assertThat(list.size).isOne();
        assertThat(list.rootNode.right).isSameAs(node);
        assertThat(list.rootNode.left).isSameAs(node);
    }

    @Test
    public void testAddNodeTwice() {
        DoubleLinkedList<String, String> list = new DoubleLinkedList<>();

        DoubleLinkedList.Node<String, String> node = DoubleLinkedList.createNode("foo", "bar");

        list.addHead(node);

        assertThatThrownBy(() -> list.addHead(node))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Can not add node twice!");

        DoubleLinkedList.Node<String, String> node2 = DoubleLinkedList.createNode("foo", "bar");

        // try to be smart.
        list.addHead(node2);

        assertThatThrownBy(() -> list.addHead(node))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Can not add node twice!");
    }

    @Test
    public void testAddRemove() {

        DoubleLinkedList<String, String> list = new DoubleLinkedList<>();
        DoubleLinkedList.Node<String, String> node = DoubleLinkedList.createNode("foo", "bar");

        list.addHead(node);

        assertThat(list.size).isOne();
        assertThat(list.rootNode.right).isSameAs(node);
        assertThat(list.rootNode.left).isSameAs(node);

        list.removeNode(node);
        assertThat(node.left).isNull();
        assertThat(node.right).isNull();

        assertThat(list.size).isZero();
        assertThat(list.rootNode.right).isSameAs(list.rootNode);
        assertThat(list.rootNode.left).isSameAs(list.rootNode);
    }

    @Test
    public void testAddRemoveTail() {

        DoubleLinkedList<String, String> list = new DoubleLinkedList<>();
        DoubleLinkedList.Node<String, String> node = DoubleLinkedList.createNode("foo", "bar");

        list.addHead(node);

        assertThat(list.size).isOne();
        assertThat(list.rootNode.right).isSameAs(node);
        assertThat(list.rootNode.left).isSameAs(node);

        list.removeTail();
        assertThat(node.left).isNull();
        assertThat(node.right).isNull();

        assertThat(list.size).isZero();
        assertThat(list.rootNode.right).isSameAs(list.rootNode);
        assertThat(list.rootNode.left).isSameAs(list.rootNode);
    }

    @Test
    public void testAddTwiceRemoveTail() {

        DoubleLinkedList<String, String> list = new DoubleLinkedList<>();
        DoubleLinkedList.Node<String, String> node1 = DoubleLinkedList.createNode("foo", "bar");
        DoubleLinkedList.Node<String, String> node2 = DoubleLinkedList.createNode("foo", "bar");

        list.addHead(node1);
        list.addHead(node2);

        assertThat(list.size).isEqualTo(2);
        assertThat(list.rootNode.right).isSameAs(node2);
        assertThat(list.rootNode.left).isSameAs(node1);

        // remove tail node
        list.removeNode(node1);
        assertThat(node1.left).isNull();
        assertThat(node1.right).isNull();

        assertThat(list.size).isOne();
        assertThat(list.rootNode.right).isSameAs(node2);
        assertThat(list.rootNode.left).isSameAs(node2);
    }

    @Test
    public void testAddTwiceRemoveHead() {

        DoubleLinkedList<String, String> list = new DoubleLinkedList<>();
        DoubleLinkedList.Node<String, String> node1 = DoubleLinkedList.createNode("foo", "bar");
        DoubleLinkedList.Node<String, String> node2 = DoubleLinkedList.createNode("foo", "bar");

        list.addHead(node1);
        list.addHead(node2);

        assertThat(list.size).isEqualTo(2);
        assertThat(list.rootNode.right).isSameAs(node2);
        assertThat(list.rootNode.left).isSameAs(node1);

        // remove tail node
        list.removeNode(node2);
        assertThat(node2.left).isNull();
        assertThat(node2.right).isNull();

        assertThat(list.size).isOne();
        assertThat(list.rootNode.right).isSameAs(node1);
        assertThat(list.rootNode.left).isSameAs(node1);
    }

    @Test
    public void testAddThreeRemoveMiddle() {

        DoubleLinkedList<String, String> list = new DoubleLinkedList<>();
        DoubleLinkedList.Node<String, String> node1 = DoubleLinkedList.createNode("foo", "bar");
        DoubleLinkedList.Node<String, String> node2 = DoubleLinkedList.createNode("foo", "bar");
        DoubleLinkedList.Node<String, String> node3 = DoubleLinkedList.createNode("foo", "bar");

        list.addHead(node1);
        list.addHead(node2);
        list.addHead(node3);

        assertThat(list.size).isEqualTo(3);
        assertThat(list.rootNode.right).isSameAs(node3);
        assertThat(list.rootNode.left).isSameAs(node1);

        // remove middle node
        list.removeNode(node2);
        assertThat(node2.left).isNull();
        assertThat(node2.right).isNull();

        assertThat(list.size).isEqualTo(2);
        assertThat(list.rootNode.right).isSameAs(node3);
        assertThat(list.rootNode.left).isSameAs(node1);
    }

}
