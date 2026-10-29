package com.als.presence.domain.model
data class LoggedDevice(val id:String,val deviceName:String?=null,val lastActiveAt:String?=null,val isCurrent:Boolean=false)
