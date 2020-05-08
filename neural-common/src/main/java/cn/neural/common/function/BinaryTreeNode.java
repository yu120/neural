package cn.neural.common.function;

import lombok.Data;

@Data
public class BinaryTreeNode<T> {

    /**
     * 数据
     */
    private T data;
    /**
     * 左孩子
     */
    private BinaryTreeNode<T> leftChild;
    /**
     * 右孩子
     */
    private BinaryTreeNode<T> rightChild;


}