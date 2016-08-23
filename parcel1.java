=== java
##读信息
dataSize()   (mDataSize > mDataPos ? mDataSize : mDataPos);
dataAvail()		dataSize() - dataPosition()	
dataPosition()   mDataPos
dataCapacity()		//可用空间

写信息
setDataPosition(int pos)
setDataCapacity(int size)   // 设置新的 可用大小，不能比 dataSize 小


【保证能写入前4字节】调用流 (反)
writeInt()--> jni --> writeInt32() --> writeAligned



template<class T>
status_t Parcel::writeAligned(T val) {
    COMPILE_TIME_ASSERT_FUNCTION_SCOPE(PAD_SIZE(sizeof(T)) == sizeof(T));

    if ((mDataPos+sizeof(val)) <= mDataCapacity) {
restart_write:
        *reinterpret_cast<T*>(mData+mDataPos) = val;
        return finishWrite(sizeof(val));
    }

    status_t err = growData(sizeof(val));
    if (err == NO_ERROR) goto restart_write;
    return err;
}

status_t Parcel::readInt32(int32_t *pArg) const
{
    return readAligned(pArg);
}





//漏洞代码
//const size_t size = p->readInt32();     //1
// +    const void* regionData = p->readInplace(size);   //2
// +    if (regionData == NULL) {
// +        return NULL;
// +    }
// region->readFromMemory(regionData, size);       //3

template<class T>
status_t Parcel::readAligned(T *pArg) const {
    COMPILE_TIME_ASSERT_FUNCTION_SCOPE(PAD_SIZE(sizeof(T)) == sizeof(T));

    if ((mDataPos+sizeof(T)) <= mDataSize) {
        const void* data = mData+mDataPos;
        mDataPos += sizeof(T);
        *pArg =  *reinterpret_cast<const T*>(data);
        return NO_ERROR;
    } else {
        return NOT_ENOUGH_DATA;
    }
}

const void* Parcel::readInplace(size_t len) const
{
    if ((mDataPos+PAD_SIZE(len)) >= mDataPos && (mDataPos+PAD_SIZE(len)) <= mDataSize     //false  关键
            && len <= PAD_SIZE(len)) {
        const void* data = mData+mDataPos;
        mDataPos += PAD_SIZE(len);
        ALOGV("readInplace Setting data pos of %p to %zu", this, mDataPos);
        return data;
    }
    return NULL;
}


===============
template<class T>
status_t Parcel::writeAligned(T val) {
    COMPILE_TIME_ASSERT_FUNCTION_SCOPE(PAD_SIZE(sizeof(T)) == sizeof(T));

    if ((mDataPos+sizeof(val)) <= mDataCapacity) {
restart_write:
        *reinterpret_cast<T*>(mData+mDataPos) = val;
        return finishWrite(sizeof(val));
    }

    status_t err = growData(sizeof(val));
    if (err == NO_ERROR) goto restart_write;
    return err;
}

