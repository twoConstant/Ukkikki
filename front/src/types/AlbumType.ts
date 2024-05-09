// Props Type

export interface DetailImgProps {
  url: string
}


// API type

export interface FileDownloadDto {
  fileId: number;
  prefix: string;
}

export interface MultiFileDownloadDto {
  fileIdList: number[];
  prefix: string;
}

export interface contentListData {
  type: string;
  pk: string;
  name: string;
  url: string;
}

export interface AlbumData {
  parentId: string;
  contentList: contentListData[]
}

export interface AlbumResponse {
  resultCode: string;
  data: AlbumData
}