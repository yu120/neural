package cn.neural.common;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 排序算法
 *
 * @author lry
 */
public class NeuralSort {

    /**
     * 冒泡排序
     * <p>
     * 每次选出单轮中最大的数值,并移动到最后(最右边)
     *
     * @param data 待排序数组
     */
    public static void bubbleSort(int[] data) {
        if (data.length <= 1) {
            return;
        }

        for (int i = 0; i < data.length - 1; i++) {
            // 用于标记单轮比较是否有交换操作,无交换操作则表示已排好序
            boolean flag = false;
            // 把最大的找出来，放至最后
            for (int j = 0; j < data.length - 1 - i; j++) {
                // 当前数比后一个数大，则交换
                if (data[j] > data[j + 1]) {
                    int temp = data[j];
                    data[j] = data[j + 1];
                    data[j + 1] = temp;
                    flag = true;
                }
            }
            // 没有需要交换的，则表示已排好序
            if (!flag) {
                return;
            }
        }
    }

    /**
     * 选择排序
     * <p>
     * 每次选出单轮中最小数值的索引值,并移动到最前(最左边)
     *
     * @param data 待排序数组
     */
    public static void selectSort(int[] data) {
        if (data.length <= 1) {
            return;
        }

        for (int i = 0; i < data.length - 1; i++) {
            // 默认第1个为本轮最小值的索引
            int minIndex = i;

            // 每次从剩余数中筛选出最小数的索引
            for (int j = i + 1; j < data.length; j++) {
                // 当前数值小于本次最小值,则设置为最小值索引
                if (data[j] < data[minIndex]) {
                    minIndex = j;
                }
            }

            // 最小索引值发生了变化，则交换
            if (minIndex != i) {
                int temp = data[i];
                data[i] = data[minIndex];
                data[minIndex] = temp;
            }
        }
    }

    /**
     * 插入排序
     *
     * @param data 待排序数组
     */
    public static void insertSort(int[] data) {
        if (data.length <= 1) {
            return;
        }

        // 从第一个开始计算
//        for (int i = 1; i < data.length; i++) {
//            // 从当前位置开始向前比较：相邻两个相互比较大小 TODO:可以采用二分法来查找有序数组
//            for (int j = i; j > 0; j--) {
//                // 前一个大于当前数，则交换（小往前，大往后）
//                if (data[j - 1] > data[j]) {
//                    int temp = data[j - 1];
//                    data[j - 1] = data[j];
//                    data[j] = temp;
//                } else {
//                    // 第一次出现前一个比后一个小，则结束
//                    break;
//                }
//            }
//        }

        for (int i = 1; i < data.length; i++) {
            if (data[i - 1] > data[i]) {
                int temp = data[i];
                int j;
                for (j = i - 1; j >= 0 && data[j] > temp; j--) {
                    data[j + 1] = data[j];
                }
                data[j + 1] = temp;
            }
        }
    }

    /**
     * 快速排序
     *
     * @param data 待排序数组
     */
    public static void quickSort(int[] data) {
        if (data.length <= 1) {
            return;
        }

        recursiveQuickSort(0, data.length - 1, data);
    }

    /**
     * 递归快速排序
     *
     * @param low
     * @param high
     * @param data 待排序数组
     */
    private static void recursiveQuickSort(int low, int high, int[] data) {
        if (low < high) {
            // 寻找基准值应该正确的索引位置
            int index = getQuickSortIndex(low, high, data);

            // 对基准数左边部分进行递归快速排序
            recursiveQuickSort(0, index - 1, data);

            // 对基准数右边部分进行递归快速排序
            recursiveQuickSort(index + 1, high, data);
        }
    }

    /**
     * 计算出基准值low应该所在的正确位置的索引
     *
     * @param low
     * @param high
     * @param data 待排序数组
     * @return 返回low应该在的正确索引位置
     */
    private static int getQuickSortIndex(int low, int high, int[] data) {
        // 选取low位置作为基准
        int temp = data[low];
        while (low < high) {
            // high位置的数大于等于基准值,则high指针向左移动
            while (low < high && data[high] >= temp) {
                high--;
            }
            // 跳出上述循环，则表示high位置的数小于了基准值，则直接将high位置的数移动到low位置
            data[low] = data[high];

            // low位置的数小于等于基准值,则low指针向右移动
            while (low < high && data[low] <= temp) {
                low++;
            }
            // 跳出上述循环，则表示low位置的数大于了基准值，则直接将low位置的数移动到high位置
            data[high] = data[low];
        }
        // 上述的基准值
        data[low] = temp;

        return low;
    }

    /**
     * 计数排序
     *
     * @param data 待排序数组
     */
    public static void countingSort(int[] data) {
        if (data.length <= 1) {
            return;
        }

        // 寻找最大值和最小值
        int min = data[0], max = data[0];
        for (int i = 1; i < data.length; i++) {
            if (max < data[i]) {
                max = data[i];
            } else if (data[i] < min) {
                min = data[i];
            }
        }

        // 分组统计每个数出现的次数
        int[] temp = new int[max - min + 1];
        for (int datum : data) {
            temp[datum - min] = temp[datum - min] + 1;
        }

        // 对temp进行遍历，temp[i]的值就是i出现的次数
        int k = 0;
        for (int i = 0; i < temp.length; i++) {
            for (int j = temp[i]; j > 0; j--) {
                data[k] = i + min;
                k++;
            }
        }
    }

    /**
     * 基数排序
     * <p>
     * 1.按照个位数分组,然后按照数组索引大小依次取出作为新数组
     * 2.按照十位数分组,然后按照数组索引大小依次取出作为新数组
     * 3.按照百位数分组,然后按照数组索引大小依次取出作为新数组
     * ......
     *
     * @param data 待排序数组
     */
    public static void radixSort(int[] data) {
        if (data.length <= 1) {
            return;
        }

        // Step1: 查找数组中的最大值
        int max = data[0];
        for (int i = 1; i < data.length - 1; i++) {
            if (data[i] > max) {
                max = data[i];
            }
        }

        // Step2: 计算最大值的位数
        int maxDigit = 0;
        while (max > 0) {
            max /= 10;
            maxDigit++;
        }

        // 定义每一轮的除数: 1,10,100...
        int divisor = 1;
        // 定义了10个桶
        int[][] bucket = new int[10][data.length];
        // 统计每个桶中实际存放的元素个数
        int[] count = new int[bucket.length];

        // Step3: 按位分组排序
        for (int i = 1; i <= maxDigit; i++) {
            // Step4: 分组入桶
            for (int temp : data) {
                // 计算入桶位置
                int digit = (temp / divisor) % 10;
                // count[digit]++表示单组的索引位置
                bucket[digit][count[digit]++] = temp;
            }

            // Step5: 从0到9号桶按照顺序取出
            int k = 0;
            for (int b = 0; b < bucket.length; b++) {
                // 如果这个桶中没有元素放入，那么跳过
                if (count[b] == 0) {
                    continue;
                }

                // 把单个桶中的所有数按下标逐一取出，放入单次结果数组中
                for (int w = 0; w < count[b]; w++) {
                    data[k++] = bucket[b][w];
                }

                // 桶中的元素已经全部取出，计数器归零
                count[b] = 0;
            }

            // 除数准一增加
            divisor *= 10;
        }
    }

    /**
     * 希尔排序
     * <p>
     * 对交换式的希尔排序进行优化->移位法
     *
     * @param data 待排序数组
     */
    public static void shellSort(int[] data) {
        // 增量gap, 并逐步的缩小增量
        for (int gap = data.length / 2; gap > 0; gap /= 2) {
            // 从第gap个元素，逐个对其所在的组进行直接插入排序
            for (int i = gap; i < data.length; i++) {
                int j = i;
                int temp = data[j];
                if (data[j] < data[j - gap]) {
                    while (j - gap >= 0 && temp < data[j - gap]) {
                        // 移动
                        data[j] = data[j - gap];
                        j -= gap;
                    }
                    // 当退出while后，就给temp找到插入的位置
                    data[j] = temp;
                }
            }
        }
    }

    /**
     * 堆排序
     *
     * @param data 待排序数组
     */
    public static void heapSort(int[] data) {
        if (data.length <= 1) {
            return;
        }

        // 构造初始堆,从第一个非叶子节点开始调整,左右孩子节点中较大的交换到父节点中
        for (int i = (data.length) / 2 - 1; i >= 0; i--) {
            heapAdjust(data, data.length, i);
        }
        // 排序，将最大的节点放在堆尾，然后从根节点重新调整
        for (int i = data.length - 1; i >= 1; i--) {
            int temp = data[0];
            data[0] = data[i];
            data[i] = temp;
            heapAdjust(data, i, 0);
        }
    }

    private static void heapAdjust(int[] list, int len, int i) {
        int k = i, temp = list[i], index = 2 * k + 1;
        while (index < len) {
            if (index + 1 < len) {
                if (list[index] < list[index + 1]) {
                    index = index + 1;
                }
            }
            if (list[index] > temp) {
                list[k] = list[index];
                k = index;
                index = 2 * k + 1;
            } else {
                break;
            }
        }
        list[k] = temp;
    }

    /**
     * 归并排序
     *
     * @param data 待排序数组
     */
    public static void mergeSort(int[] data) {
        if (data.length <= 1) {
            return;
        }
        mergeSort(data, 0, data.length - 1);
    }

    private static void mergeSort(int[] data, int start, int end) {
        // 当子序列中只有一个元素时结束递归
        if (start < end) {
            // 划分子序列
            int mid = (start + end) / 2;
            // 对左侧子序列进行递归排序
            mergeSort(data, start, mid);
            // 对右侧子序列进行递归排序
            mergeSort(data, mid + 1, end);
            // 合并
            merge(data, start, mid, end);
        }
    }

    /**
     * 两路归并算法，两个排好序的子序列合并为一个子序列
     *
     * @param data  待排序数组
     * @param left
     * @param mid
     * @param right
     */
    private static void merge(int[] data, int left, int mid, int right) {
        // 辅助数组
        int[] tmp = new int[data.length];
        // p1、p2是检测指针，k是存放指针
        int p1 = left, p2 = mid + 1, k = left;
        while (p1 <= mid && p2 <= right) {
            if (data[p1] <= data[p2]) {

                tmp[k++] = data[p1++];
            } else {
                tmp[k++] = data[p2++];
            }
        }

        while (p1 <= mid) {
            // 如果第一个序列未检测完，直接将后面所有元素加到合并的序列中
            tmp[k++] = data[p1++];
        }
        while (p2 <= right) {
            // 同上
            tmp[k++] = data[p2++];
        }

        // 复制回原素组
        for (int i = left; i <= right; i++) {
            data[i] = tmp[i];
        }
    }

    /**
     * 桶排序
     *
     * @param data 待排序数组
     */
    public static void bucketSort(int[] data) {
        if (data.length <= 1) {
            return;
        }


    }

    public static void main(String[] args) {
        int[] data = {49, 38, 65, 97, 23, 22, 76, 1, 5, 8, 2, 0, 22, 100, 101};
        insertSort(data);
        System.out.println(Arrays.stream(data).boxed().collect(Collectors.toList()));
    }

}
